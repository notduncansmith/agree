-- :name new-user :i!
insert into users (created_at, username, access_token)
           values (:created-at, :username, :access-token);

-- :name new-claim :i!
insert into claims (created_at, author_id, `text`)
            values (:created-at, :author-id, :text);

-- :name new-vote :i!
insert into votes (created_at, claim_id, user_id, direction)
           values (:created-at, :claim-id, :user-id, :direction);

-- :name new-score :i!
insert into scores (latest_scored_claim_id, karma, streak, user_id)
            values (:latest-scored-claim-id, :karma, :streak, :user-id);

-- :name set-claim-totals :!
update claims set final_up = :final-up, final_down = :final-down where id = :id;

-- :name set-vote-direction :!
update votes set direction = :direction
  where user_id = :user-id
    and claim_id = :claim-id;

-- :name update-score-for-user :!
update scores
  set latest_scored_claim_id = :latest-scored-claim-id,
      karma = :karma,
      streak = :streak
  where user_id = :user-id;