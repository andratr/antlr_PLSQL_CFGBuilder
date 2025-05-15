DECLARE
    v_is_active CHAR(1) := 'Y';
    v_message   VARCHAR2(100);
BEGIN
    DBMS_OUTPUT.PUT_LINE('Start');

    IF v_is_active = 'Y' THEN
        BEGIN
            DBMS_OUTPUT.PUT_LINE('Inside IF');
            v_message := 'Active';
            INSERT INTO some_table VALUES ('test');  -- any real statement
        END;
    END IF;

    DBMS_OUTPUT.PUT_LINE('Message: ' || v_message);
END;
