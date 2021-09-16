// This is an open source non-commercial project. Dear PVS-Studio, please check it.
// PVS-Studio Static Code Analyzer for C, C++ and C#: http://www.viva64.com

package interfacebuilder.compress;

import com.ahli.mpq.mpqeditor.MpqEditorCompressionRule;
import com.ahli.mpq.mpqeditor.MpqEditorCompressionRuleParser;
import org.hibernate.Hibernate;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "rule_set")
public final class RuleSet implements Serializable {
	
	@Serial
	private static final long serialVersionUID = -8442665535029507145L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", nullable = false)
	private Long id;
	
	@Transient
	private MpqEditorCompressionRule[] compressionRules;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "rule_set_compression_rules_string", joinColumns = { @JoinColumn(name = "rule_set_id") })
	@Column(name = "compression_rules_string")
	private List<String> compressionRulesString;
	
	protected RuleSet() {
		// required for hibernate
	}
	
	public RuleSet(final MpqEditorCompressionRule... compressionRules) {
		setCompressionRulesPrivate(compressionRules);
	}
	
	@Transient
	private void setCompressionRulesPrivate(final MpqEditorCompressionRule... compressionRules) {
		this.compressionRules = compressionRules;
		
		// update string representation in DB
		final List<String> rulesStrings = new ArrayList<>(compressionRules.length);
		for (final MpqEditorCompressionRule compressionRule : compressionRules) {
			rulesStrings.add(compressionRule.toString());
		}
		compressionRulesString = rulesStrings;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(final Long id) {
		this.id = id;
	}
	
	@Transient
	public MpqEditorCompressionRule[] getCompressionRules() throws IOException {
		// lazy-load array
		if (compressionRules == null) {
			compressionRules = new MpqEditorCompressionRule[getCompressionRulesString().size()];
			for (int i = 0; i < compressionRules.length; ++i) {
				compressionRules[i] = MpqEditorCompressionRuleParser.parse(compressionRulesString.get(i));
			}
		}
		return compressionRules;
	}
	
	@Transient
	public void setCompressionRules(final MpqEditorCompressionRule... compressionRules) {
		setCompressionRulesPrivate(compressionRules);
	}
	
	public List<String> getCompressionRulesString() {
		return compressionRulesString;
	}
	
	public void setCompressionRulesString(final List<String> compressionRulesString) {
		this.compressionRulesString = compressionRulesString;
	}
	
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		final RuleSet ruleSet = (RuleSet) o;
		// only compare primary keys
		return Objects.equals(id, ruleSet.id);
	}
	
	@Override
	public int hashCode() {
		// for generated primary keys, the hashcode must be constant before and after
		return 0;
	}
}
