DECLARE
  counter NUMBER := 0;
BEGIN
  LOOP
    counter := counter + 1;

    IF counter = 2 THEN
      CONTINUE;
    ELSIF counter = 3 THEN
      EXIT;
    ELSIF counter = 4 THEN
      RETURN;
    END IF;

    DBMS_OUTPUT.PUT_LINE('Counter: ' || counter);
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('Loop ended');
END;
