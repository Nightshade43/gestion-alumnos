# Scaffold de /frontend

Estos archivos van en la **raíz de `/frontend`** dentro del repo del backend. El código de
la app (`src/`) y los tokens vienen del paquete de handoff.

## Instalación

Desde la raíz del repo `gestion-alumnos`:

```bash
mkdir -p frontend
cp -r design_handoff_gestion_alumnos/scaffold/. frontend/
cp -r design_handoff_gestion_alumnos/src/. frontend/src/
mkdir -p frontend/src/styles
cp design_handoff_gestion_alumnos/tokens/tokens.css frontend/src/styles/tokens.css
cp design_handoff_gestion_alumnos/tokens/tokens.ts  frontend/src/styles/tokens.ts

cd frontend
npm install
npm run dev
```

Queda así:

```
frontend/
├─ index.html
├─ package.json
├─ vite.config.ts          ← proxy /api → localhost:8080
├─ tsconfig.json
├─ tsconfig.node.json
├─ .env.example
└─ src/
   ├─ main.tsx             ← importa styles/tokens.css y monta <App />
   ├─ App.tsx              ← guard de sesión + router mínimo
   ├─ api/                 ← client, endpoints, queries, types
   ├─ components/          ← primitives, Toasts, ErrorSurface
   ├─ screens/             ← las 11 pantallas
   ├─ shell/AppShell.tsx
   └─ styles/tokens.css
```

**Importante**: `primitives.tsx` importa `tokens.ts` como `'../../tokens/tokens'`. Al
mover los tokens a `src/styles/`, ese import pasa a `'../styles/tokens'`. Es el único
ajuste de rutas del scaffold.

## Verificar que arrancó bien

1. Backend corriendo en `localhost:8080` con los datos de `seed-data.sh` cargados.
2. `npm run dev` → `http://localhost:5173`.
3. Login con el usuario del `AdminUserSeeder`. Si el login responde y aparece el sidebar,
   el proxy y el JWT están bien.
4. Comparar cada pantalla con `design/screens/` y con el prototipo `design/App.dc.html`.

## Sobre CORS

No hace falta tocar el backend mientras se trabaje con el proxy: el navegador ve todo en
`localhost:5173`. Cuando se despliegue con el backend en otro origen, definir
`VITE_API_BASE` y agregar la config de CORS en Spring — es el único cambio necesario.
