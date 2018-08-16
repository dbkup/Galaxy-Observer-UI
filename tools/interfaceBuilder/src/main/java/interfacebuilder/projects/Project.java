package interfacebuilder.projects;

import interfacebuilder.compress.RuleSet;
import interfacebuilder.projects.enums.Game;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table (name = "project")
public class Project {
	@Id
	@GeneratedValue (strategy = GenerationType.AUTO)
	private Integer id;
	
	@Column (unique = false, length = 30, nullable = false)
	private String name;
	
	@Column (unique = true, length = 255, nullable = false)
	private String projectPath;
	
	@Enumerated (EnumType.STRING)
	@Column (length = 8, nullable = false)
	private Game game;
	
	@Column
	private Date lastBuildDate;
	
	@Column
	private Long lastBuildSize;
	
	@OneToOne (fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private RuleSet bestCompressionRuleSet;
	
	public Project() {
		// required
	}
	
	public Project(final String name, final String projectPath, final Game game) {
		this.name = name;
		this.projectPath = projectPath;
		this.game = game;
	}
	
	@Override
	public String toString() {
		return String.format("Project{id=%s, projectPath=%s, game=%s}", id, projectPath, game);
	}
	
	public Integer getId() {
		return id;
	}
	
	public void setId(final Integer id) {
		this.id = id;
	}
	
	public String getProjectPath() {
		return projectPath;
	}
	
	public void setProjectPath(final String projectPath) {
		this.projectPath = projectPath;
	}
	
	public Game getGame() {
		return game;
	}
	
	public void setGame(final Game game) {
		this.game = game;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(final String name) {
		this.name = name;
	}
	
	public Date getLastBuildDate() {
		return lastBuildDate;
	}
	
	public void setLastBuildDate(final Date lastBuildDate) {
		this.lastBuildDate = lastBuildDate;
	}
	
	public Long getLastBuildSize() {
		return lastBuildSize;
	}
	
	public void setLastBuildSize(final long lastBuildSize) {
		this.lastBuildSize = lastBuildSize;
	}
	
	/**
	 * Returns the RuleSet with the best compression.
	 *
	 * @return
	 */
	public RuleSet getBestCompressionRuleSet() {
		return bestCompressionRuleSet;
	}
	
	public void setBestCompressionRuleSet(final RuleSet bestCompressionRuleSet) {
		this.bestCompressionRuleSet = bestCompressionRuleSet;
	}
}