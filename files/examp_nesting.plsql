DECLARE
    v_value NUMBER := 100;
    v_result NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('Outer block start');

    BEGIN
        DBMS_OUTPUT.PUT_LINE('Middle block');

        BEGIN
            IF v_value > 0 THEN
                v_result := v_value * 2;
                DBMS_OUTPUT.PUT_LINE('Deep block: result = ' || v_result);
            ELSE
                v_result := 0;
            END IF;
        END;

    EXCEPTION
        WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Exception in middle block');
    END;

    DBMS_OUTPUT.PUT_LINE('Outer block end');
EXCEPTION
    WHEN ZERO_DIVIDE THEN
        DBMS_OUTPUT.PUT_LINE('Division by zero in outer block');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Unhandled exception in outer block');
END;
