-- Sample data for the remaining tables: addresses, carts, cart_items, orders, order_items, payments.
-- Prerequisite: run db/sample-data.sql first (needs the sample user and the books).
-- Run:  psql -h localhost -d bookstore -v ON_ERROR_STOP=1 -f db/sample-data-orders.sql
-- Safe to re-run: does nothing if the sample user already has addresses or orders.
--
-- Amounts follow the app's rules: tax = 8% of subtotal, shipping = 0 when subtotal >= free-shipping
-- threshold. Stock is reduced for the DELIVERED and PENDING orders (cancelled orders restore stock).

DO $$
DECLARE
    v_user      bigint;
    v_ship_addr bigint;
    v_bill_addr bigint;
    v_cart      bigint;
    v_order     bigint;
    p_hobbit    bigint; p_pride bigint; p_gone bigint; p_ender bigint; p_dune bigint; p_atomic bigint;
BEGIN
    SELECT id INTO v_user FROM users WHERE email = 'aarav.sharma@example.com';
    IF v_user IS NULL THEN
        RAISE EXCEPTION 'Sample user not found - run db/sample-data.sql first';
    END IF;
    IF EXISTS (SELECT 1 FROM addresses WHERE user_id = v_user) OR EXISTS (SELECT 1 FROM orders WHERE user_id = v_user) THEN
        RAISE NOTICE 'Sample addresses/orders already present - skipping';
        RETURN;
    END IF;

    SELECT id INTO p_hobbit FROM products WHERE isbn = '9780547928227';
    SELECT id INTO p_pride  FROM products WHERE isbn = '9780141439518';
    SELECT id INTO p_gone   FROM products WHERE isbn = '9780307588371';
    SELECT id INTO p_ender  FROM products WHERE isbn = '9780812550702';
    SELECT id INTO p_dune   FROM products WHERE isbn = '9780441013593';
    SELECT id INTO p_atomic FROM products WHERE isbn = '9781847941831';
    IF p_hobbit IS NULL OR p_pride IS NULL OR p_gone IS NULL OR p_ender IS NULL OR p_dune IS NULL OR p_atomic IS NULL THEN
        RAISE EXCEPTION 'Sample books not found - run db/sample-data.sql first';
    END IF;

    -- ------------------------------------------------------------ addresses
    INSERT INTO addresses (user_id, type, full_name, phone, line1, line2, city, state, postal_code, country, is_default, created_at, updated_at)
    VALUES (v_user, 'SHIPPING', 'Aarav Sharma', '+919876543210', '12, MG Road', 'Near Metro Station',
            'Bengaluru', 'Karnataka', '560001', 'IN', true, now(), now())
    RETURNING id INTO v_ship_addr;

    INSERT INTO addresses (user_id, type, full_name, phone, line1, line2, city, state, postal_code, country, is_default, created_at, updated_at)
    VALUES (v_user, 'BILLING', 'Aarav Sharma', '+919876543210', '45, Linking Road', NULL,
            'Mumbai', 'Maharashtra', '400050', 'IN', true, now(), now())
    RETURNING id INTO v_bill_addr;

    -- ---------------------------------------------------------- cart + items
    INSERT INTO carts (user_id, created_at, updated_at) VALUES (v_user, now(), now())
    ON CONFLICT (user_id) DO UPDATE SET updated_at = now()
    RETURNING id INTO v_cart;

    INSERT INTO cart_items (cart_id, product_id, quantity, created_at, updated_at) VALUES
        (v_cart, p_dune,   1, now(), now()),
        (v_cart, p_atomic, 2, now(), now())
    ON CONFLICT (cart_id, product_id) DO NOTHING;

    -- ------------------------------------------------ order 1: DELIVERED, paid by UPI
    -- subtotal 499 + 299 = 798.00, shipping 0.00, tax 63.84, total 861.84
    INSERT INTO orders (user_id, status, shipping_address_id, billing_address_id, subtotal, shipping_cost, tax, total_amount, notes, created_at, updated_at)
    VALUES (v_user, 'DELIVERED', v_ship_addr, v_bill_addr, 798.00, 0.00, 63.84, 861.84, 'Gift wrap please',
            now() - interval '30 days', now() - interval '25 days')
    RETURNING id INTO v_order;
    INSERT INTO order_items (order_id, product_id, product_title, quantity, unit_price, subtotal, created_at, updated_at) VALUES
        (v_order, p_hobbit, 'The Hobbit',          1, 499.00, 499.00, now() - interval '30 days', now() - interval '30 days'),
        (v_order, p_pride,  'Pride and Prejudice', 1, 299.00, 299.00, now() - interval '30 days', now() - interval '30 days');
    INSERT INTO payments (order_id, method, amount, currency, status, transaction_id, created_at, updated_at)
    VALUES (v_order, 'UPI', 861.84, 'INR', 'SUCCESS', 'txn_sample_upi_0001', now() - interval '30 days', now() - interval '30 days');
    UPDATE products SET stock_quantity = stock_quantity - 1 WHERE id IN (p_hobbit, p_pride);

    -- ------------------------------------------------ order 2: PENDING, not yet paid
    -- subtotal 449.00, shipping 0.00, tax 35.92, total 484.92
    INSERT INTO orders (user_id, status, shipping_address_id, billing_address_id, subtotal, shipping_cost, tax, total_amount, created_at, updated_at)
    VALUES (v_user, 'PENDING', v_ship_addr, v_bill_addr, 449.00, 0.00, 35.92, 484.92, now() - interval '1 day', now() - interval '1 day')
    RETURNING id INTO v_order;
    INSERT INTO order_items (order_id, product_id, product_title, quantity, unit_price, subtotal, created_at, updated_at) VALUES
        (v_order, p_gone, 'Gone Girl', 1, 449.00, 449.00, now() - interval '1 day', now() - interval '1 day');
    UPDATE products SET stock_quantity = stock_quantity - 1 WHERE id = p_gone;

    -- ------------------------------------------------ order 3: CANCELLED, payment refunded
    -- subtotal 499.00, shipping 0.00, tax 39.92, total 538.92 (stock was restored on cancel)
    INSERT INTO orders (user_id, status, shipping_address_id, billing_address_id, subtotal, shipping_cost, tax, total_amount, cancellation_reason, created_at, updated_at)
    VALUES (v_user, 'CANCELLED', v_ship_addr, v_bill_addr, 499.00, 0.00, 39.92, 538.92, 'Ordered by mistake',
            now() - interval '10 days', now() - interval '9 days')
    RETURNING id INTO v_order;
    INSERT INTO order_items (order_id, product_id, product_title, quantity, unit_price, subtotal, created_at, updated_at) VALUES
        (v_order, p_ender, 'Ender''s Game', 1, 499.00, 499.00, now() - interval '10 days', now() - interval '10 days');
    INSERT INTO payments (order_id, method, amount, currency, status, transaction_id, created_at, updated_at)
    VALUES (v_order, 'CREDIT_CARD', 538.92, 'INR', 'REFUNDED', 'txn_sample_card_0002', now() - interval '10 days', now() - interval '9 days');
END $$;
