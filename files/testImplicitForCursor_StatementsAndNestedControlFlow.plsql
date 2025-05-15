SET SERVEROUTPUT ON;

DECLARE
  -- Implicit cursor FOR loop with SELECT
  v_count NUMBER := 0;

  -- Explicit cursor using %ROWTYPE
  CURSOR emp_cursor IS
    SELECT employee_id, last_name, department_id
    FROM employees
    WHERE ROWNUM <= 5;

  emp_rec emp_cursor%ROWTYPE;

BEGIN
  -- Cursor FOR loop using SELECT directly
  FOR emp IN (SELECT employee_id, last_name FROM employees WHERE department_id = 10) LOOP
    DBMS_OUTPUT.PUT_LINE('FOR-SELECT: ' || emp.employee_id || ' - ' || emp.last_name);
    v_count := v_count + 1;
  END LOOP;

  DBMS_OUTPUT.PUT_LINE('---');

  -- Explicit cursor with %ROWTYPE
  OPEN emp_cursor;
  LOOP
    FETCH emp_cursor INTO emp_rec;
    EXIT WHEN emp_cursor%NOTFOUND;

    DBMS_OUTPUT.PUT_LINE('Explicit: ' || emp_rec.employee_id || ' - ' || emp_rec.last_name || ', Dept: ' || emp_rec.department_id);
  END LOOP;
  CLOSE emp_cursor;

  DBMS_OUTPUT.PUT_LINE('Total rows (FOR-SELECT): ' || v_count);
END;
/
