DECLARE
  score NUMBER := 75;
  extra_credit BOOLEAN := TRUE;
BEGIN
  IF score >= 90 THEN
    DBMS_OUTPUT.PUT_LINE('Grade: A');
  ELSIF score >= 80 THEN
    DBMS_OUTPUT.PUT_LINE('Grade: B');
  ELSIF score >= 70 THEN
    IF extra_credit THEN
      DBMS_OUTPUT.PUT_LINE('Grade: C+ (with extra credit)');
    ELSIF score >= 75 THEN
      DBMS_OUTPUT.PUT_LINE('Grade: C');
    ELSE
      DBMS_OUTPUT.PUT_LINE('Grade: C-');
    END IF;
  ELSE
    DBMS_OUTPUT.PUT_LINE('Grade: F');
  END IF;

  DBMS_OUTPUT.PUT_LINE('Done grading.');
END;
