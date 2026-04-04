# Configuración de Firebase para sincronización de datos

## Problema identificado
Los datos no se están sincronizando correctamente con Firebase porque faltan configuraciones importantes en Firestore.

## Solución: Configurar Firestore Rules

### 1. Abrir la consola de Firestore
1. Ve a [Firebase Console](https://console.firebase.google.com)
2. Selecciona tu proyecto `gymtracker-e4054`
3. En el menú izquierdo, selecciona **Firestore Database**
4. Si aún no existe la base de datos, créala en modo de prueba

### 2. Configurar las reglas de Firestore
1. En Firestore Database, ve a la pestaña **Rules**
2. Reemplaza el contenido actual con las siguientes reglas:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permitir acceso a los snapshots de usuarios autenticados
    match /user_snapshots/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Permitir que Firebase lea/escriba datos de autenticación
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Haz clic en **Publish** para aplicar las reglas

### 3. Verificar la configuración de OAuth
1. En Firebase Console, ve a **Authentication** -> **Sign-in method**
2. Habilita el proveedor **Google**:
   - **Cliente ID en Android**: Debe ser el de tu app (ya está configurado)
   - **Web Client ID**: 
     - Valor: `667861694298-p4tja11t6qippbcqea0m4p09ufa2m7ge.apps.googleusercontent.com`
     - Este valor debe coincidir exactamente con el de `gradle.properties`
   - **Secreto del cliente web**: Puedes dejar en blanco (no es requerido para Android)

### 4. Verificar el SHA-1 del certificado
Tu SHA-1 actual es: `98:74:7f:e7:8f:9d:59:ac:32:ac:4d:ac:e0:b0:8d:f9:8e:5e:1c:38`

Este está correctamente registrado en `google-services.json` como:
```
"certificate_hash": "98747fe78f9d59ac32ac4dace0b08df98e5e1c38"
```

### 5. Probar la sincronización
1. Desinstala la app del dispositivo
2. Ejecuta: `./gradlew clean build`
3. Instala el nuevo APK
4. Inicia sesión
5. Crea una rutina y un ejercicio
6. Abre Firebase Console -> Firestore Database -> Collections
7. Deberías ver una colección `user_snapshots` con tu documento de usuario

## Datos que se sincronizan
- Rutinas
- Ejercicios
- Entrenamientos completados
- Series realizadas

Estos datos se guardan como un snapshot en Firestore y se restauran automáticamente cuando inicias sesión desde otro dispositivo.

