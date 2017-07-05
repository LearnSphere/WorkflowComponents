-- Takes 8 seconds to execute
-- Created on July 6, 2013
-- @author: Franck for ALFA, MIT lab: franck.dernoncourt@gmail.com

set @current_date = cast('0000-00-00 00:00:00' as datetime);

SELECT features.longitudinal_feature_id, COUNT(*)

FROM moocdb.user_longitudinal_feature_values AS features
 INNER JOIN moocdb.users AS users
 ON users.user_id = features.user_id
WHERE users.user_dropout_week > 2 AND
    features.date_of_extraction = @current_date
GROUP BY features.longitudinal_feature_id
ORDER BY features.longitudinal_feature_id ASC
;
