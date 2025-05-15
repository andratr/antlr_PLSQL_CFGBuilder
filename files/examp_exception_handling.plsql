DECLARE
    acct_balance NUMBER(11, 2);
    debit_amt NUMBER(5, 2) := 500.00;
    result NUMBER;
BEGIN
    acct_balance := 1000.00;
    result := acct_balance / 0;  -- Division by zero
EXCEPTION
    WHEN ZERO_DIVIDE THEN
        DBMS_OUTPUT.PUT_LINE('Error: Division by zero is not allowed.');
        result := 0;
    WHEN INVALID_NUMBER THEN
        DBMS_OUTPUT.PUT_LINE('Error: Invalid number encountered.');
        result := 0;
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('An unexpected error occurred: ' || SQLERRM);
        result := NULL;
END;
