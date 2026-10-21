# Desbloqueo de una Pantalla (modo kiosco)

Una Pantalla se bloquea sola al arrancar y no responde a nada del teclado
salvo la secuencia de desbloqueo. El Controlador, cuando proyecta, también se
bloquea con la misma contraseña.

## Secuencia de desbloqueo por defecto

```
Shift + Shift + Ctrl + Ctrl + Alt + Shift
```

- Está compilada dentro de la aplicación.
- **Nunca se muestra en la pantalla bloqueada.** Solo aparece en este documento
  y en el botón **Seguridad** del Controlador.
- Si la pantalla **no tiene contraseña** (por ejemplo, recién instalada, en
  blanco), completar la secuencia la desbloquea directamente.
- Si la pantalla **tiene contraseña**, primero se escribe la secuencia y en
  seguida la contraseña.

## Contraseña propia

El Controlador define una contraseña con el botón **Seguridad** y la envía a las
pantallas vinculadas.

- Entre 4 y 64 caracteres, solo letras (A-Z, a-z) y números (0-9).
- Solo se guarda y se envía su hash SHA-256 junto con su longitud; la
  contraseña en claro nunca sale del Controlador.

## Frase de recuperación universal

Se escribe tras la secuencia de desbloqueo:

```
0137GenericPassword
```

Está compilada por diseño y sirve para recuperar una pantalla si se olvida la
contraseña.

## Límite de intentos

Tras 5 intentos fallidos, el desbloqueo se rechaza durante 30 segundos.

## Límites conocidos

- `Ctrl + Alt + Supr` no se puede interceptar (limitación del sistema).
- En modo `--windowed` (solo depuración) el bloqueo es "blando": algunos atajos
  del sistema (Alt+Tab, tecla Windows) pueden escapar. En modo normal, el
  enganche nativo de Windows los bloquea.
