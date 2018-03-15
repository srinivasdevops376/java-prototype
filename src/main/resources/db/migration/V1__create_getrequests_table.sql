
GRANT delete, insert, select, update ON java-prototype.* TO `${application_username}`;

-- Create syntax for 'java-prototype.getrequests'
CREATE TABLE getrequests (
  id int(11) unsigned NOT NULL AUTO_INCREMENT,
  timestamp timestamp NULL DEFAULT NULL,
  hash varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=499 DEFAULT CHARSET=utf8;
