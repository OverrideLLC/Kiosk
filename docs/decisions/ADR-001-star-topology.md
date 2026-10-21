# ADR-001: Topología en estrella (Controlador → Pantallas)

## Contexto

Un comercio tiene una o varias pantallas y una persona que administra el
contenido. Las alternativas eran:

- **P2P**: cada equipo descubre y sincroniza con cualquier otro.
- **Estrella**: un Controlador envía; las Pantallas solo reciben.
- **Servidor central**: un equipo dedicado o un servicio en la nube.

## Decisión

Topología en estrella. Solo el rol **Pantalla** se publica por mDNS y abre un
puerto. El **Controlador** es cliente y nunca acepta conexiones. La primera vez
cada equipo elige su rol.

## Consecuencias

- Es imposible que dos Controladores se vean entre sí por accidente: la regla de
  negocio queda garantizada por el diseño de descubrimiento, no por un filtro.
- La Pantalla no necesita conocer a otras Pantallas ni coordinar versiones.
- El Controlador es un punto único de administración; si se apaga, las Pantallas
  siguen reproduciendo su copia local.
- Para agregar una pantalla hay que vincularla desde el Controlador; no hay
  alta automática.
- La topología es fácil de explicar y de depurar en una LAN pequeña.
