# Configuracion Firebase Auth

Este proyecto ya tiene integrado el flujo de autenticacion con Firebase para:

- Email y contrasena
- Google
- Apple ID

## 1) Crear proyecto Firebase

1. Entra en Firebase Console y crea (o reutiliza) un proyecto.
2. Registra la app Android con el package `com.example.gimnasio`.
3. Descarga `google-services.json`.
4. Copia ese archivo en `app/google-services.json`.

## 2) Habilitar proveedores en Firebase Authentication

En Firebase Console -> Authentication -> Sign-in method:

- Habilita `Email/Password`.
- Habilita `Google`.
- Habilita `Apple` (requiere configurar Service ID / Team ID / Key ID / private key de Apple).

## 3) Configurar Google Web Client ID

Obtén el `Web client ID` (OAuth 2.0 Client IDs) y colocalo en `gradle.properties`:

```properties
GOOGLE_WEB_CLIENT_ID=TU_WEB_CLIENT_ID.apps.googleusercontent.com
```

## 4) Requisitos de compilacion

- Android Studio con JDK configurado.
- Si falta `google-services.json`, la app compila, pero Google/Apple quedan desactivados en Ajustes.
- Si falta `GOOGLE_WEB_CLIENT_ID`, Google queda desactivado en Ajustes.

## 5) Flujo esperado en la app

- `Ajustes` -> Cuenta:
  - Iniciar sesion / Crear cuenta (email)
  - Google
  - Apple ID
- Cerrar sesion limpia Firebase + sesion local en DataStore.
- Cuando un proveedor no esta disponible, el boton aparece deshabilitado con mensaje explicativo.

