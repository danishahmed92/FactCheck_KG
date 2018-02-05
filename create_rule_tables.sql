CREATE TABLE `kg_mp`.`triple_pattern_map` (
  `TPM_ID` INT NOT NULL AUTO_INCREMENT,
  `TPM_SUBJ` VARCHAR(300) NULL,
  `TPM_PRED` VARCHAR(300) NULL,
  `TPM_OBJ` VARCHAR(300) NULL,
  PRIMARY KEY (`TPM_ID`));

CREATE TABLE `kg_mp`.`kg_rule` (
  `rl_id` INT NOT NULL AUTO_INCREMENT,
  `rl_propval` VARCHAR(300) NULL,
  `rl_propfreq` INT NULL,
  PRIMARY KEY (`rl_id`));

CREATE TABLE `kg_mp`.`kg_pred` (
  `pr_id` INT NOT NULL AUTO_INCREMENT,
  `pr_uri` VARCHAR(300) NULL,
  `pr_label` VARCHAR(200) NULL,
  `pr_freq` INT NULL,
  PRIMARY KEY (`pr_id`));

CREATE TABLE `kg_mp`.`kg_rule_map` (
  `rm_id` INT NOT NULL AUTO_INCREMENT,
  `rm_tpm_id` INT NULL,
  `rm_rl_id` INT NULL,
  PRIMARY KEY (`rm_id`),
  INDEX `RM_TPM_ID_FK_idx` (`rm_tpm_id` ASC),
  INDEX `RM_RL_ID_FK_idx` (`rm_rl_id` ASC),
  CONSTRAINT `RM_TPM_ID_FK`
    FOREIGN KEY (`rm_tpm_id`)
    REFERENCES `kg_mp`.`triple_pattern_map` (`TPM_ID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `RM_RL_ID_FK`
    FOREIGN KEY (`rm_rl_id`)
    REFERENCES `kg_mp`.`kg_rule` (`rl_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `kg_mp`.`kg_pred_map` (
  `PM_ID` INT NOT NULL AUTO_INCREMENT,
  `pm_rl_id` INT NULL,
  `pm_pr_id` INT NULL,
  PRIMARY KEY (`PM_ID`),
  INDEX `PM_RL_ID_FK_idx` (`pm_rl_id` ASC),
  INDEX `PM_PR_ID_FK_idx` (`pm_pr_id` ASC),
  CONSTRAINT `PM_RL_ID_FK`
    FOREIGN KEY (`pm_rl_id`)
    REFERENCES `kg_mp`.`kg_rule` (`rl_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `PM_PR_ID_FK`
    FOREIGN KEY (`pm_pr_id`)
    REFERENCES `kg_mp`.`kg_pred` (`pr_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `kg_mp`.`kg_rule_sigval_map` (
  `RSM_ID` INT NOT NULL AUTO_INCREMENT,
  `RSM_RL_ID` INT NULL,
  `RSM_SIGVAL` DOUBLE NULL,
  PRIMARY KEY (`RSM_ID`),
  INDEX `RSM_RL_ID_FK_idx` (`RSM_RL_ID` ASC),
  CONSTRAINT `RSM_RL_ID_FK`
    FOREIGN KEY (`RSM_RL_ID`)
    REFERENCES `kg_mp`.`kg_rule` (`rl_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `kg_mp`.`kg_pred_bucket` (
  `pb_id` INT NOT NULL AUTO_INCREMENT,
  `pb_pr_id` INT NULL,
  `pb_uri` VARCHAR(500) NULL,
  PRIMARY KEY (`pb_id`),
  INDEX `PB_PR_ID_FK_idx` (`pb_pr_id` ASC),
  CONSTRAINT `PB_PR_ID_FK`
    FOREIGN KEY (`pb_pr_id`)
    REFERENCES `kg_mp`.`kg_pred` (`pr_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
PACK_KEYS = 1;

CREATE TABLE `kg_mp`.`kg_rule_propval_bucket` (
  `RPB_ID` INT NOT NULL AUTO_INCREMENT,
  `RPB_RL_ID` INT NULL,
  `RPB_PROPVAL` VARCHAR(500) NULL,
  PRIMARY KEY (`RPB_ID`),
  INDEX `RPB_RL_ID_FK_idx` (`RPB_RL_ID` ASC),
  CONSTRAINT `RPB_RL_ID_FK`
    FOREIGN KEY (`RPB_RL_ID`)
    REFERENCES `kg_mp`.`kg_rule` (`rl_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

ALTER TABLE `kg_mp`.`kg_pred` 
CHANGE COLUMN `pr_uri` `pr_uri` VARCHAR(500) NULL DEFAULT NULL ,
CHANGE COLUMN `pr_label` `pr_label` VARCHAR(500) NULL DEFAULT NULL ;

ALTER TABLE `kg_mp`.`kg_rule` 
CHANGE COLUMN `rl_propval` `rl_propval` VARCHAR(500) NULL DEFAULT NULL ;