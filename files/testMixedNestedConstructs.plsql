DECLARE
  i NUMBER := 0;
BEGIN
  LOOP
    i := i + 1;

    IF i = 1 THEN
      DBMS_OUTPUT.PUT_LINE('i is 1');
    ELSIF i = 2 THEN
      IF MOD(i, 2) = 0 THEN
        DBMS_OUTPUT.PUT_LINE('even 2');
        RETURN;
      ELSE
        DBMS_OUTPUT.PUT_LINE('not even 2');
      END IF;
    ELSIF i = 3 THEN
      DBMS_OUTPUT.PUT_LINE('three');
      EXIT;
    ELSE
      DBMS_OUTPUT.PUT_LINE('other');
    END IF;

    EXIT WHEN i > 5;
  END LOOP;
END;
