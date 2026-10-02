#!/bin/bash
# Se ejecuta UNA vez, cuando el volumen de MongoDB está vacío.
# Crea el usuario que usa la API con permisos de lectura/escritura SOLO sobre la base mano_amiga (5.6):
# no puede administrar el servidor ni tocar otras bases.
set -e
mongosh --quiet -u "$MONGO_INITDB_ROOT_USERNAME" -p "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin <<EOF
use mano_amiga
db.createUser({
  user: "$MONGO_APP_USER",
  pwd: "$MONGO_APP_PASSWORD",
  roles: [{ role: "readWrite", db: "mano_amiga" }]
})
EOF
