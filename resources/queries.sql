-- :name score-for-user
select * from scores where user_id = :user-id;

-- :name claims-since-timestamp :? :*
select * from claims where created_at > :since;

-- :name get-all-users :? :*
select id, username, created_at, access_token from users;

-- :name get-all-scores :? :*
select latest_scored_claim_id, user_id, streak, karma from scores;

-- :name votes-since-timestamp :? :*
select * from votes where created_at > :since;

-- :name user-votes-since-claim-id :? :*
select * from votes where user_id = :user-id
                      and claim_id > :claim-id;