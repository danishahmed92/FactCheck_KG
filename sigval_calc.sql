insert into kg_rule_sigval_map (rsm_rl_id, rsm_sigval)
SELECT 
    r1.rl_id,
    CAST(r1.rl_propfreq / (SELECT 
                SUM(r2.rl_propfreq)
            FROM
                kg_rule r2,
                kg_rule_map rm2,
                triple_pattern_map tpm2
            WHERE
                r2.rl_id = rm2.rm_rl_id
                    AND rm2.rm_tpm_id = tpm2.tpm_id
                    AND tpm1.TPM_ID = tpm2.TPM_ID)
        AS DECIMAL (10 , 8 )) AS sigVal
FROM
    kg_rule r1,
    kg_rule_map rm1,
    triple_pattern_map tpm1
WHERE
    r1.rl_id = rm1.rm_rl_id
        AND rm1.rm_tpm_id = tpm1.tpm_id;