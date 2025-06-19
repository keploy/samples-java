-- init.sql
DO $$ 
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'library_demo') THEN
        RAISE NOTICE 'Creating database: library_demo';
  CREATE DATABASE library_demo;
  ELSE
    RAISE NOTICE 'Database library_demo already exists, skipping creation';
  END IF;
END $$;

