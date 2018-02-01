package bean;

// default package
// Generated Feb 1, 2018 1:02:35 AM by Hibernate Tools 5.2.8.Final

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * KgRuleSigvalMap generated by hbm2java
 */
@Entity
@Table(name = "kg_rule_sigval_map", catalog = "kg_mp")
public class KgRuleSigvalMap implements java.io.Serializable {

	private int rsmId;
	private KgRule kgRule;
	private Double rsmSigval;

	public KgRuleSigvalMap() {
	}

	public KgRuleSigvalMap(int rsmId) {
		this.rsmId = rsmId;
	}

	public KgRuleSigvalMap(KgRule kgRule, Double rsmSigval) {
		this.rsmId = rsmId;
		this.kgRule = kgRule;
		this.rsmSigval = rsmSigval;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "RSM_ID", unique = true, nullable = false)
	public int getRsmId() {
		return this.rsmId;
	}

	public void setRsmId(int rsmId) {
		this.rsmId = rsmId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "RSM_RL_ID")
	public KgRule getKgRule() {
		return this.kgRule;
	}

	public void setKgRule(KgRule kgRule) {
		this.kgRule = kgRule;
	}

	@Column(name = "RSM_SIGVAL", precision = 22, scale = 0)
	public Double getRsmSigval() {
		return this.rsmSigval;
	}

	public void setRsmSigval(Double rsmSigval) {
		this.rsmSigval = rsmSigval;
	}

}