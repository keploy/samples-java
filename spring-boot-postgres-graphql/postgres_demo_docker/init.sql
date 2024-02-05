-- init.sql
DO $$ 
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'library_demo') THEN
    CREATE DATABASE library_demo;
  END IF;
END $$;

