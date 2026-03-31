-- Migración de la versión 1 (Legacy SQLite) a la versión 2 (Room)
-- Los datos se mueven programáticamente desde ListaFeeds.db y Feed.db en AppDatabase.java
-- Este archivo queda como base para futuras modificaciones estructurales.

-- Ejemplo de futuras operaciones:
-- ALTER TABLE fuentes ADD COLUMN sincronizacion_automatica INTEGER DEFAULT 0;
