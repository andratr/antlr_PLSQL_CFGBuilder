DECLARE
  v_score     NUMBER := 85;
  v_status    VARCHAR2(50);
  v_category  VARCHAR2(50);
  some_date   DATE := SYSDATE + 1;
BEGIN
  -- CASE expression with SELECT in THEN
  v_status := CASE
    WHEN v_score >= 90 THEN (SELECT 'Top' FROM dual)
    WHEN v_score >= 70 THEN (SELECT 'Above Average' FROM dual)
    ELSE (SELECT 'Needs Improvement' FROM dual)
  END;

  -- CASE expression with nested CASE
  v_category := CASE
    WHEN v_score >= 80 THEN
      CASE
        WHEN SYSDATE < some_date THEN 'Early High'
        ELSE 'Late High'
      END
    ELSE 'Low'
  END;

  -- CASE statement with DML in THEN branches
  CASE
    WHEN v_score >= 90 THEN
      INSERT INTO score_log VALUES ('Insert: High', SYSDATE);
    WHEN v_score >= 80 THEN
      UPDATE score_log SET log_time = SYSDATE WHERE log_text LIKE '%High%';
    ELSE
      DELETE FROM score_log WHERE log_text = 'Old';
  END CASE;

  -- CASE inside IF
  IF v_score > 80 THEN
    v_status := CASE
      WHEN v_score >= 95 THEN 'Outstanding'
      WHEN v_score >= 85 THEN 'Excellent'
      ELSE 'Good'
    END;
  END IF;

  -- IF inside CASE statement
  CASE
    WHEN v_score < 50 THEN
      IF v_score < 30 THEN
        INSERT INTO score_log VALUES ('Very Low', SYSDATE);
      END IF;
    ELSE
      NULL;
  END CASE;

  DBMS_OUTPUT.PUT_LINE('Status: ' || v_status);
  DBMS_OUTPUT.PUT_LINE('Category: ' || v_category);
END;
/
