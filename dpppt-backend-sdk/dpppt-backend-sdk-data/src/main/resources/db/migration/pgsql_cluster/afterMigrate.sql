create or replace procedure reassign_objects_ownership() LANGUAGE 'plpgsql'
as $BODY$
BEGIN
	IF EXISTS(SELECT FROM pg_catalog.pg_roles where rolname = current_user || '_role_full') THEN
		execute format('reassign owned by %s to %s_role_full', user, current_database()); 
	END IF;
END $BODY$;


call reassign_objects_ownership();

drop procedure reassign_objects_ownership();