DECLARE
    acct_balance NUMBER(11, 2);
    debit_amt NUMBER(5, 2) := 500.00;
    acct NUMBER(4) := 3;
BEGIN
    -- FOR loop example
    FOR i IN 1..10 LOOP
        -- Do something with the loop index i
        acct_balance := acct_balance + i;
    END LOOP;

    -- WHILE loop example
    WHILE debit_amt > 0 LOOP
        -- Decrement the debit amount
        debit_amt := debit_amt - 50;
        acct_balance := acct_balance + 50;
    END LOOP;

    -- Commit to finalize changes
    COMMIT;
END;
