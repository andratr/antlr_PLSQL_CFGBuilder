DECLARE
   qty_on_hand  NUMBER(5);
   order_status VARCHAR2(20);
   error_msg    VARCHAR2(100);
BEGIN
   BEGIN
      SELECT quantity INTO qty_on_hand FROM inventory
         WHERE product = 'TENNIS RACKET'
         FOR UPDATE OF quantity;

      IF qty_on_hand > 0 THEN
         order_status := 'In Stock';

         -- Nested IF block for order validation
         IF qty_on_hand > 5 THEN
            UPDATE inventory SET quantity = quantity - 1
               WHERE product = 'TENNIS RACKET';
            INSERT INTO purchase_record VALUES ('Tennis racket purchased', SYSDATE);
         ELSE
            INSERT INTO purchase_record VALUES ('Tennis racket reserved', SYSDATE);
         END IF;

      ELSE
         order_status := 'Out of Stock';
         INSERT INTO purchase_record VALUES ('Out of tennis rackets', SYSDATE);
      END IF;

   EXCEPTION
      WHEN NO_DATA_FOUND THEN
         error_msg := 'Product not found';
         INSERT INTO error_log (error_message, timestamp) VALUES (error_msg, SYSDATE);
      WHEN OTHERS THEN
         error_msg := 'An unexpected error occurred';
         INSERT INTO error_log (error_message, timestamp) VALUES (error_msg, SYSDATE);
   END;

   -- Loop for some other business logic
   FOR i IN 1..5 LOOP
      BEGIN
         -- Another nested block inside loop
         IF i = 3 THEN
            -- Simulate an operation that might fail
            RAISE_APPLICATION_ERROR(-20001, 'Simulated error on iteration ' || i);
         ELSE
            DBMS_OUTPUT.PUT_LINE('Iteration: ' || i);
         END IF;
      EXCEPTION
         WHEN OTHERS THEN
            DBMS_OUTPUT.PUT_LINE('Error on iteration ' || i);
      END;
   END LOOP;

   COMMIT;

EXCEPTION
   WHEN OTHERS THEN
      DBMS_OUTPUT.PUT_LINE('General error handling');
      ROLLBACK;
END;
