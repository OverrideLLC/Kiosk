# Protocolo de red

## Transporte

- HTTP/1.1 sobre la red local, sin TLS (decisión consciente; ver
  [SECURITY.md](SECURITY.md)).
- Cuerpos JSON en UTF-8 (`application/json`) salvo la subida de imágenes, que es
  binaria.
- Puerto inicial de la Pantalla: **8765**. Si está ocupado, prueba 8766, 8767…
  hasta encontrar uno libre y anuncia el real por mDNS.
- Timeouts del cliente: 5 s para peticiones normales, 60 s para sincronización y
  120 s por imagen.
- No hay nube, cuentas ni llamadas a internet.

## Descubrimiento (mDNS)

La Pantalla publica:

```
Tipo:     _kiosksync._tcp.local.
Instancia: KioskSync-<primeros 8 caracteres del deviceId>
Puerto:   puerto real
TXT:      deviceId=<uuid>
          deviceName=<nombre del equipo>
          role=receiver
          paired=true|false
```

El Controlador busca ese tipo y **no publica nada**. Por eso dos Controladores no
pueden verse entre sí. Si mDNS falla, el Controlador puede llamar directamente a
`GET /info` sobre una IP y puerto escritos a mano.

## Autenticación

Todas las rutas excepto `GET /info` y `POST /pair` exigen:

```
X-Emitter-Id: <id del Controlador vinculado>
X-Auth-Token: <token de 32 bytes en hex entregado al vincular>
```

Si falta cualquiera de los dos o no coincide con `paired.json`, la respuesta es:

```http
401 Unauthorized
{"error":"unauthorized"}
```

La comparación del token y del `emitterId` se hace en tiempo constante.

## Formato de error

```json
{
  "error": "invalid_code",
  "message": "texto opcional",
  "remainingAttempts": 4,
  "remainingSeconds": 25
}
```

Solo el campo `error` es obligatorio. `remainingAttempts` y `remainingSeconds`
aparecen en los errores de vinculación.

## Endpoints

| Método y ruta | Auth | Función |
| --- | --- | --- |
| `GET /info` | No | Identidad y estado de la Pantalla. |
| `POST /pair` | Código | Vincula un Controlador y devuelve el token. |
| `DELETE /pair` | Sí | Desvincula. |
| `GET /status` | Sí | Estado de reproducción. |
| `GET /sync/state` | Sí | Imágenes y versión que la Pantalla ya tiene. |
| `PUT /media/{mediaId}` | Sí | Sube una imagen. |
| `PUT /playlist` | Sí | Aplica una playlist completa. |
| `PUT /lock` | Sí | Configura el bloqueo tipo kiosco. |

### GET /info

```http
200 OK
{
  "deviceId": "3f1c…",
  "deviceName": "Pantalla sala",
  "role": "receiver",
  "paired": false,
  "appVersion": "1.0.0"
}
```

Es la única ruta que un Controlador puede consultar sin token. Sirve para el plan B
de IP manual y para comprobar que el destino no es otro Controlador.

### POST /pair

```json
{
  "emitterId": "8a2e…",
  "emitterName": "Caja principal",
  "code": "483920"
}
```

| Resultado | Código | Cuerpo |
| --- | --- | --- |
| Correcto | `200` | `{"token":"<64 hex>","receiverId":"<uuid>"}` |
| Petición inválida | `400` | `{"error":"invalid_request"}` |
| Código incorrecto | `401` | `{"error":"invalid_code","remainingAttempts":4}` |
| Demasiados intentos | `429` | `{"error":"locked_out","remainingSeconds":30}` |
| Ya vinculada | `409` | `{"error":"already_paired"}` |

Reglas:

- Solo se acepta si la Pantalla **no** está vinculada y está en modo vinculación.
- El código tiene 6 dígitos y vale **5 minutos**; al vencer se renueva.
- Máximo **5 intentos fallidos**. Al alcanzarlos se genera un código nuevo y se
  rechazan solicitudes durante **30 segundos**.
- Al vincular se genera un token aleatorio de 32 bytes (64 caracteres hex), se
  guarda con el `emitterId` en `paired.json` y se devuelve una sola vez.

### DELETE /pair

```http
200 OK
ok
```

Borra `paired.json` en la Pantalla y vuelve al modo vinculación con un código
nuevo. El Controlador debe olvidar el token correspondiente. También existe el
atajo local `Ctrl+Shift+P` en la Pantalla, con confirmación.

### GET /status

```http
200 OK
{
  "playlistVersion": 3,
  "currentMediaId": "m2",
  "currentFileName": "promo-2x1.png"
}
```

El Controlador lo consulta cada 5 segundos. Tres fallos seguidos marcan la pantalla
como desconectada; la Pantalla sigue reproduciendo igual.

### GET /sync/state

```http
200 OK
{
  "playlistVersion": 3,
  "media": [
    {"mediaId":"m1","sha256":"<64 hex>"},
    {"mediaId":"m2","sha256":"<64 hex>"}
  ]
}
```

`playlistVersion` es 0 si la Pantalla no tiene playlist. `media` lista los archivos
guardados, no solo los referenciados.

### PUT /media/{mediaId}

- `mediaId`: `^[A-Za-z0-9_-]{1,100}$`.
- Cuerpo: bytes de la imagen (JPG, PNG o WEBP, máximo 25 MB).
- Header obligatorio `X-Content-Sha256` con el SHA-256 en hex del cuerpo.

```http
200 OK
{"mediaId":"m1","sha256":"<64 hex>","sizeBytes":184320}
```

| Código | Error | Causa |
| --- | --- | --- |
| `400` | `invalid_media_id` | El id no cumple el formato. |
| `400` | `missing_hash` | Falta el header o no tiene 64 caracteres. |
| `400` | `invalid_body` | Cuerpo vacío o ilegible. |
| `400` | `hash_mismatch` | El SHA-256 calculado no coincide. |
| `413` | `image_too_large` | Supera 25 MB. |
| `415` | `unsupported_format` | No es JPG, PNG ni WEBP. |
| `500` | `storage_error` | No se pudo escribir en disco. |

La Pantalla escribe en un archivo temporal y solo lo publica si el hash y el
formato son correctos. Una subida repetida con el mismo hash es idempotente.

### PUT /playlist

```json
{
  "playlist": {
    "id": "p1",
    "name": "Promociones",
    "version": 4,
    "defaultDurationSec": 10,
    "items": [
      {"mediaId":"m1","fileName":"ficha.png","sha256":"<64 hex>","durationSec":null},
      {"mediaId":"m2","fileName":"promo.png","sha256":"<64 hex>","durationSec":6}
    ]
  },
  "lock": {
    "passwordHash": "<64 hex o null>",
    "passwordLength": 6,
    "enabled": true,
    "unlockSequence": ["SHIFT","SHIFT","CTRL","CTRL","ALT","SHIFT"],
    "recoveryHash": null,
    "recoveryLength": 0
  }
}
```

`lock` es opcional: si viene, la Pantalla guarda también la configuración de
bloqueo. La playlist solo se aplica si **todas** las imágenes referenciadas ya
están guardadas y su hash coincide.

| Resultado | Código | Cuerpo |
| --- | --- | --- |
| Aplicada | `200` | `{"applied":true,"version":4}` |
| Inválida | `400` | `{"error":"invalid_playlist"}` o `{"error":"invalid_lock"}` |
| Faltan imágenes | `409` | `{"applied":false,"version":3,"missingMediaIds":["m2"]}` |

En el caso `409` la Pantalla conserva intacta la playlist anterior. Cuando la
aplicación es correcta, guarda el manifiesto, borra las imágenes huérfanas y avisa
al reproductor sin cortar la imagen actual.

Validaciones: `version >= 1`; duración por defecto entre 3 y 600 s; `mediaId`
único; nombre de archivo sin rutas ni caracteres prohibidos; `sha256` de 64
caracteres; duración por imagen nula o entre 3 y 600 s.

### PUT /lock

```http
200 OK
ok
```

Cuerpo: un `KioskLockConfig` como el de arriba. Se usa cuando el Controlador
cambia la contraseña o la secuencia sin reenviar la playlist. `enabled` debe ser
`true`; la secuencia debe tener entre 2 y 12 teclas de `SHIFT`, `CTRL` y `ALT`.
La contraseña y la recuperación se envían como SHA-256 hex (nunca en claro), con
su longitud para dibujar los puntos.

## Secuencia de sincronización

```
Controlador                                   Pantalla
    │                                            │
    ├── GET /sync/state ────────────────────────►│
    │◄──────────── versión + media existente ────┤
    │                                            │
    │   SyncPlanner: subir solo faltantes        │
    │                                            │
    ├── PUT /media/m2 (hash) ───────────────────►│  verifica hash y formato
    │◄────────────────────────────── 200 ────────┤
    │                                            │
    ├── PUT /playlist (versión nueva) ──────────►│  valida que todo exista
    │◄────────────────── 200 applied:true ───────┤  guarda y cambia el carrusel
    │                                            │
```

Si el proceso se corta antes del `PUT /playlist`, la Pantalla sigue con la versión
anterior. Las imágenes ya subidas quedan como huérfanas y se borran en la
siguiente aplicación correcta.

## Versionado y compatibilidad

- La versión del protocolo es implícita: los campos nuevos deben ser opcionales en
  la deserialización para no romper clientes viejos.
- `appVersion` en `/info` permite diagnosticar diferencias, pero el MVP no rechaza
  versiones distintas.
- Solo hay un Controlador vinculado por Pantalla; no se soportan escrituras
  concurrentes de varios Controladores.
- La playlist local del Controlador es la única fuente de verdad.
