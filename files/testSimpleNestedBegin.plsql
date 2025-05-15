BEGIN
    DBMS_OUTPUT.PUT_LINE('Start of outer block');
    BEGIN DBMS_OUTPUT.PUT_LINE('Hello from nested block'); END;
    DBMS_OUTPUT.PUT_LINE('Back to outer block');
END;
