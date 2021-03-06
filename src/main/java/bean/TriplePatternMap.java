package bean;

// default package
// Generated Feb 1, 2018 1:02:35 AM by Hibernate Tools 5.2.8.Final

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * TriplePatternMap generated by hbm2java
 */
@Entity
@Table(name = "triple_pattern_map", catalog = "kg_mp")
public class TriplePatternMap implements java.io.Serializable {

	private int tpmId;
	private String tpmSubj;
	private String tpmPred;
	private String tpmObj;

	public TriplePatternMap() {
	}

	public TriplePatternMap(int tpmId) {
		this.tpmId = tpmId;
	}

	public TriplePatternMap(String tpmSubj, String tpmPred, String tpmObj) {
		this.tpmSubj = tpmSubj;
		this.tpmPred = tpmPred;
		this.tpmObj = tpmObj;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "TPM_ID", unique = true, nullable = false)
	public int getTpmId() {
		return this.tpmId;
	}

	public void setTpmId(int tpmId) {
		this.tpmId = tpmId;
	}

	@Column(name = "TPM_SUBJ", length = 300)
	public String getTpmSubj() {
		return this.tpmSubj;
	}

	public void setTpmSubj(String tpmSubj) {
		this.tpmSubj = tpmSubj;
	}

	@Column(name = "TPM_PRED", length = 300)
	public String getTpmPred() {
		return this.tpmPred;
	}

	public void setTpmPred(String tpmPred) {
		this.tpmPred = tpmPred;
	}

	@Column(name = "TPM_OBJ", length = 300)
	public String getTpmObj() {
		return this.tpmObj;
	}

	public void setTpmObj(String tpmObj) {
		this.tpmObj = tpmObj;
	}

}
