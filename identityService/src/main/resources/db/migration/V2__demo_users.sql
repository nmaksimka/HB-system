INSERT INTO users (id, name, birth_date) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Maxim Nikonov', DATE '2000-07-03'),
    ('22222222-2222-2222-2222-222222222222', 'Anna Petrova', DATE '2001-02-28'),
    ('33333333-3333-3333-3333-333333333333', 'Ivan Sidorov', DATE '1999-12-20')
ON CONFLICT (id) DO NOTHING;

INSERT INTO friend_groups (id, name, owner_id) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TSU Friends', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (name) DO NOTHING;

INSERT INTO group_memberships (id, group_id, user_id) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333')
ON CONFLICT (group_id, user_id) DO NOTHING;
