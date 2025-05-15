DECLARE
  CURSOR emp_cursor IS
    SELECT employee_id,
           CURSOR(SELECT department_id FROM departments WHERE manager_id = e.employee_id)
    FROM employees e WHERE ROWNUM <= 3;

  emp_id employees.employee_id%TYPE;
  dept_id departments.department_id%TYPE;
  dept_cursor SYS_REFCURSOR;
BEGIN
  OPEN emp_cursor;
  LOOP
    FETCH emp_cursor INTO emp_id, dept_cursor;
    EXIT WHEN emp_cursor%NOTFOUND;

    LOOP
      FETCH dept_cursor INTO dept_id;
      EXIT WHEN dept_cursor%NOTFOUND;

      IF dept_id > 100 THEN
        DBMS_OUTPUT.PUT_LINE('Big dept');
      END IF;
    END LOOP;
    CLOSE dept_cursor;

    DBMS_OUTPUT.PUT_LINE('Employee: ' || emp_id);
  END LOOP;
  CLOSE emp_cursor;
END;
