package ws.prova.reference2.builtins;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ws.prova.agent2.ProvaReagent;
import ws.prova.kernel2.ProvaConstant;
import ws.prova.kernel2.ProvaDerivationNode;
import ws.prova.kernel2.ProvaGoal;
import ws.prova.kernel2.ProvaKnowledgeBase;
import ws.prova.kernel2.ProvaList;
import ws.prova.kernel2.ProvaLiteral;
import ws.prova.kernel2.ProvaObject;
import ws.prova.kernel2.ProvaPredicate;
import ws.prova.kernel2.ProvaRule;
import ws.prova.kernel2.ProvaVariable;
import ws.prova.kernel2.ProvaVariablePtr;
import ws.prova.reference2.ProvaConstantImpl;
import ws.prova.reference2.ProvaListImpl;
import ws.prova.reference2.ProvaLiteralImpl;
import ws.prova.reference2.ProvaPredicateImpl;
import ws.prova.reference2.ProvaRuleImpl;

public class ProvaParseNvImpl extends ProvaBuiltinImpl {

	public ProvaParseNvImpl(ProvaKnowledgeBase kb) {
		super(kb,"parse_nv");
	}

	@Override
	public boolean process(ProvaReagent prova, ProvaDerivationNode node,
			ProvaGoal goal, List<ProvaLiteral> newLiterals, ProvaRule query) {
		ProvaLiteral literal = goal.getGoal();
		List<ProvaVariable> variables = query.getVariables();
		ProvaList terms = literal.getTerms();
		ProvaObject[] data = terms.getFixed();
		if( data.length!=2 || !(data[0] instanceof ProvaList) || !(data[1] instanceof ProvaList) )
			return false;
		ProvaObject[] out = ((ProvaList) ((ProvaList) data[1]).cloneWithVariables(variables)).getFixed();
		if( out.length!=2 )
			return false;
		ProvaObject n_out = out[0];
		if( n_out instanceof ProvaVariablePtr ) {
			ProvaVariablePtr varPtr = (ProvaVariablePtr) n_out;
			n_out = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
		}
		if( !(n_out instanceof ProvaVariable) && !(n_out instanceof ProvaList))
			return false;
		ProvaObject v_out = out[1];
		if( v_out instanceof ProvaVariablePtr ) {
			ProvaVariablePtr varPtr = (ProvaVariablePtr) v_out;
			v_out = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
		}
		if( !(v_out instanceof ProvaVariable) && !(v_out instanceof ProvaList))
			return false;
		ProvaObject[] args = ((ProvaList) ((ProvaList) data[0]).cloneWithVariables(variables)).getFixed();
		if( args.length!=2 )
			return false;
		ProvaObject lt = args[0];
		if( lt instanceof ProvaVariablePtr ) {
			ProvaVariablePtr varPtr = (ProvaVariablePtr) lt;
			lt = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
		}
		if( !(lt instanceof ProvaConstant) )
			return false;
		String in = ((ProvaConstant) lt).toString();
		ProvaObject exp = args[1];
		if( exp instanceof ProvaVariablePtr ) {
			ProvaVariablePtr varPtr = (ProvaVariablePtr) exp;
			exp = variables.get(varPtr.getIndex()).getRecursivelyAssigned();
		}
		String regexp = ((ProvaConstant) exp).toString();
		Pattern p = Pattern.compile(regexp);
		Matcher m = p.matcher(in);
		ProvaObject arr_n[] = new ProvaObject[m.groupCount()];
		ProvaObject arr_v[] = new ProvaObject[m.groupCount()];
		int i = 0;
		while (m.find()) {
			arr_n[i] = ProvaConstantImpl.create(m.group(1));
			arr_v[i++] = ProvaConstantImpl.create(m.group(2));
		}
		ProvaList n = ProvaListImpl.create( arr_n );
		ProvaList v = ProvaListImpl.create( arr_v );
		if( n_out instanceof ProvaList || v_out instanceof ProvaList ) {
			// Make sure the unification is done between the result and the original subgoal
			ProvaPredicate pred = new ProvaPredicateImpl("",2,kb);
			ProvaList ls = ProvaListImpl.create(
					new ProvaObject[] {
							data[0],
							ProvaListImpl.create(new ProvaObject[]{n,v})} );
			ProvaLiteral lit = new ProvaLiteralImpl(pred,ls);
			ProvaRule clause = ProvaRuleImpl.createVirtualRule(1, lit, null);
			pred.addClause(clause);
			ProvaLiteral newLiteral = new ProvaLiteralImpl(pred,terms);
			newLiterals.add(newLiteral);
			return true;
		}
		((ProvaVariable) n_out).setAssigned(n);
		((ProvaVariable) v_out).setAssigned(v);
		return true;
	}

}
