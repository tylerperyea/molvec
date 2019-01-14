package tripod.molvec.algo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import tripod.molvec.util.ConnectionTable;

public class FragmentTest {

	
	private File getFile(String fname){
		ClassLoader classLoader = getClass().getClassLoader();
		return new File(classLoader.getResource(fname).getFile());
		
	}
	
	@Test
	public void thinBenzeneShouldHave6BondsWith3DoubleAnd3Single() throws Exception {
		File f=getFile("fragmentTest/benzene.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		
		
		assertEquals(6,ctab.getNodes().size());
		assertEquals(6,ctab.getEdges().size());	
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
	}
	
	@Test
	public void thickTolueneShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/wiggly.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		
		
		assertEquals(7,ctab.getNodes().size());
		assertEquals(7,ctab.getEdges().size());	
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(4,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
		assertEquals(7,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
	}
	
	@Test
	public void smallRingSystemWithDashedBondsShouldHaveCorrectCarbonBonds() throws Exception {
		File f=getFile("fragmentTest/cyclopropane.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		assertEquals(5,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
		assertEquals(5,ctab.getEdges().stream().filter(e->e.getOrder()==1)
				                               .filter(e->e.getRealNode1().getSymbol().equals("C") && e.getRealNode2().getSymbol().equals("C"))
				                               .count());
	}
	@Test
	public void smallRingSystemWithDashedBondsShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/cyclopropane.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		assertEquals(9,ctab.getNodes().size());
		assertEquals(10,ctab.getEdges().size());
		assertEquals(10,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getDashed()).count());
		assertEquals(5,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
		assertEquals(2,ctab.getNodes().stream().filter(n->n.getSymbol().equals("N")).count());
		assertEquals(2,ctab.getNodes().stream().filter(n->n.getSymbol().equals("H")).count());
	}
	
	
	@Test
	public void complexThinStructureShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/thin_with_spaces2.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		assertEquals(13,ctab.getNodes().size());
		assertEquals(14,ctab.getEdges().size());
		assertEquals(8,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
		assertEquals(4,ctab.getNodes().stream().filter(n->n.getSymbol().equals("N")).count());
		assertEquals(1,ctab.getNodes().stream().filter(n->n.getSymbol().equals("S")).count());
		assertEquals(4,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(10,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
	}
	
	@Test
	public void thickSubstitutedNapthaShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/wiggly2.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		
		
		assertEquals(11,ctab.getNodes().size());
		assertEquals(12,ctab.getEdges().size());	
		assertEquals(5,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(7,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());

		assertEquals(11,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
	}
	
	@Test
	public void bromineContainingCompoundShouldHaveBromine() throws Exception {
		File f=getFile("fragmentTest/bromineContaining.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		assertEquals(7,ctab.getNodes().size());
		assertEquals(7,ctab.getEdges().size());	
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(4,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
		assertEquals(1,ctab.getNodes().stream().filter(n->n.getSymbol().equals("Br")).count());
		assertEquals(6,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
	}
	
	@Test
	public void multiRingFragmentWithOxygensShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/frag1.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		assertEquals(10,ctab.getNodes().size());
		assertEquals(11,ctab.getEdges().size());	
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(8,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
		assertEquals(3,ctab.getNodes().stream().filter(n->n.getSymbol().equals("O")).count());
		assertEquals(7,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
	}
	
	@Test
	public void thinFragmentWithLargerSpacesShouldHaveCorrectReading() throws Exception {
		File f=getFile("fragmentTest/thin_with_spaces.png");
		StructureImageExtractor sie = new StructureImageExtractor();
		sie.load(f);
		ConnectionTable ctab = sie.getCtab();
		
		
		assertEquals(13,ctab.getNodes().size());
		assertEquals(14,ctab.getEdges().size());	
		assertEquals(3,ctab.getEdges().stream().filter(e->e.getOrder()==2).count());
		assertEquals(11,ctab.getEdges().stream().filter(e->e.getOrder()==1).count());
		assertEquals(1,ctab.getNodes().stream().filter(n->n.getSymbol().equals("O")).count());
		assertEquals(2,ctab.getNodes().stream().filter(n->n.getSymbol().equals("N")).count());
		assertEquals(10,ctab.getNodes().stream().filter(n->n.getSymbol().equals("C")).count());
	}
	
	
}
