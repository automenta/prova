package ws.prova.reference2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.prova.kernel2.ProvaConstant;
import ws.prova.kernel2.ProvaGoal;
import ws.prova.kernel2.ProvaKnowledgeBase;
import ws.prova.kernel2.ProvaLiteral;
import ws.prova.kernel2.ProvaObject;
import ws.prova.kernel2.ProvaRule;
import ws.prova.kernel2.ProvaRuleSet;
import ws.prova.kernel2.ProvaUnification;

public class ProvaRuleSetImpl implements ProvaRuleSet {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger("prova");

	private final String symbol;

	private final int arity;
	
	private final List<ProvaRule> clauses = new ArrayList<ProvaRule>();
	
	private final Map<Object,List<ProvaRule>> firstArgMap = new HashMap<Object,List<ProvaRule>>();

	private final Map<String,List<ProvaRule>> srcMap = new HashMap<String,List<ProvaRule>>();

	public ProvaRuleSetImpl(String symbol) {
		this.symbol = symbol;
		this.arity = -1;
	}

	public ProvaRuleSetImpl(String symbol, int arity) {
		this.symbol = symbol;
		this.arity = arity;
	}

	public void setClauses(List<ProvaRule> clauses) {
		this.clauses.addAll(clauses);
	}

	@Override
	public List<ProvaRule> getClauses() {
		return clauses;
	}

	@Override
	public synchronized List<ProvaRule> getClauses(Object key) {
		List<ProvaRule> bound = firstArgMap.get(key);
		List<ProvaRule> free = firstArgMap.get("@");
		if( bound==null )
			return free;
		else if( free==null )
			return bound;
		// Merge the two
		final int boundSize = bound.size();
		final int freeSize = free.size();
		List<ProvaRule> merged = new ArrayList<ProvaRule>(boundSize+freeSize);
		int i1 = 0;
		int i2 = 0;
		while( i1<boundSize && i2<freeSize ) {
			if( bound.get(i1).getRuleId()>free.get(i2).getRuleId() )
				merged.add(free.get(i2++));
			else
				merged.add(bound.get(i1++));
		}
		while( i1<boundSize ) {
			merged.add(bound.get(i1++));
		}
		while( i2<freeSize ) {
			merged.add(free.get(i2++));
		}
		return merged;
	}

	@Override
	public synchronized void removeClauses(Object key) {
		List<ProvaRule> bound = firstArgMap.get(key);
		for( ProvaRule rule : bound ) {
			rule.setRemoved();
			clauses.remove(rule);
		}
	}

	// This is only used for removing temporal rules in inlne reactions.
	// TODO: Optimise rule storage for multi-key access
	@Override
	public synchronized boolean removeClauses(Object key, int offset) {
		for( Iterator<ProvaRule> iter=clauses.iterator(); iter.hasNext(); ) {
			ProvaRule rule = iter.next();
			if( ((ProvaConstant) rule.getHead().getTerms().getFixed()[offset]).getObject().equals(key) ) {
//				log.info("Removed rule with key "+key);
				rule.setRemoved();
				iter.remove();
				return true;
			}
		}
		// TODO: Throw something, this should never happen
		return false;
	}

	/**
	 * Remove only the clause that is subsumed by the query.
	 */
	@Override
	public boolean removeClausesByMatch(ProvaKnowledgeBase kb, ProvaObject[] data) {
		ProvaLiteral goalLit = kb.generateLiteral(data);
		ProvaRule query = kb.generateGoal(new ProvaLiteral[] {goalLit});
		ProvaGoal goal = new ProvaGoalImpl(query);
        ProvaUnification unification = null;
		while( (unification = goal.nextUnification(kb))!=null ) {
			boolean result = unification.unify();
			if( !result || !unification.targetUnchanged() )
				continue;
			goal.removeTarget();
			return true;
		}
		return false;
	}

	/**
	 * Remove only the clauses that are subsumed by the query.
	 */
	@Override
	public boolean removeAllClausesByMatch(ProvaKnowledgeBase kb,
			ProvaObject[] data) {
		ProvaLiteral goalLit = kb.generateLiteral(data);
		ProvaRule query = kb.generateGoal(new ProvaLiteral[] {goalLit});
		ProvaGoal goal = new ProvaGoalImpl(query);
        ProvaUnification unification = null;
		while( (unification = goal.nextUnification(kb))!=null ) {
			boolean result = unification.unify();
			if( result && unification.targetUnchanged() )
				goal.removeTarget();
			continue;
		}
		return true;
	}

	@Override
	public String getSymbol() {
		return symbol;
	}

	@Override
	public int getArity() {
		return arity;
	}

	@Override
	public synchronized void add(ProvaRule clause) {
		final Object firstArg = clause.getFirstArg();
		if( firstArg!=null ) {
			List<ProvaRule> rules = firstArgMap.get(firstArg);
			if( rules==null ) {
				rules = new ArrayList<ProvaRule>();
				firstArgMap.put(firstArg, rules);
			}
			rules.add(clause);
		}
		// TODO: understand the implications of this
		if( !clauses.contains(clause) )
			clauses.add(clause);
	}

	@Override
	public synchronized void addA(ProvaRule clause) {
		final Object firstArg = clause.getFirstArg();
		if( firstArg!=null ) {
			List<ProvaRule> rules = firstArgMap.get(firstArg);
			if( rules==null ) {
				rules = new ArrayList<ProvaRule>();
				firstArgMap.put(firstArg, rules);
			}
			rules.add(0,clause);
		}
		// TODO: understand the implications of this
		if( !clauses.contains(clause) )
			clauses.add(0,clause);
	}

	@Override
	public Object size() {
		return clauses.size();
	}

	@Override
	public synchronized void addAll(ProvaRuleSet ruleSet) {
		for( ProvaRule clause : ruleSet.getClauses() )
			add(clause);
	}

	@Override
	public synchronized void addRuleToSrc(ProvaRuleImpl rule, String src) {
		List<ProvaRule> rules = srcMap.get(src);
		if( rules==null ) {
			rules = new ArrayList<ProvaRule>();
			srcMap.put(src, rules);
		}
		rules.add(rule);
	}

	@Override
	public synchronized void removeClausesBySrc(String src) {
		List<ProvaRule> rules = srcMap.remove(src);
		if( rules==null )
			return;
		for( ProvaRule rule : rules ) {
			rule.setRemoved();
			clauses.remove(rule);
		}
	}

	@Override
	public ProvaUnification nextMatch(ProvaKnowledgeBase kb,
			ProvaGoal goal) {
        ProvaUnification unification = null;
		while( (unification = goal.nextUnification(kb))!=null ) {
			boolean result = unification.unify();
			if( result )
				return unification;
			continue;
		}
		return null;
	}

}