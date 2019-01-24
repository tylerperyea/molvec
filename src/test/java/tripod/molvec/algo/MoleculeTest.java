package tripod.molvec.algo;

import static org.junit.Assert.assertEquals;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import gov.nih.ncats.chemkit.api.util.stream.ThrowingStream;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import gov.nih.ncats.chemkit.api.Chemical;
import gov.nih.ncats.chemkit.api.ChemicalBuilder;
import gov.nih.ncats.chemkit.api.inchi.Inchi;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MoleculeTest {


    private File getFile(String fname){
        ClassLoader classLoader = getClass().getClassLoader();
        return new File(classLoader.getResource(fname).getFile());

    }

    public static class TestSpec{
        private String filePath;
        private ThrowingStream.ThrowingConsumer<Chemical, Exception> assertionConsumer;

        public TestSpec(String filePath, ThrowingStream.ThrowingConsumer<Chemical, Exception> assertionConsumer) {
            this.filePath = filePath;
            this.assertionConsumer = assertionConsumer;
        }
    }





    private TestSpec spec;

    public MoleculeTest(String ignored, TestSpec spec){
        this.spec = spec;
    }


    @Test
    public void testAsFile() throws Exception {
        File f=getFile(spec.filePath);

        StructureImageExtractor sie = new StructureImageExtractor(f);
        spec.assertionConsumer.accept(sie.getChemical());
    }
    @Test
    public void testAsByteArray() throws Exception {
        File f=getFile(spec.filePath);


        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try(InputStream in = new BufferedInputStream(new FileInputStream(f))){
            byte[] buf = new byte[1024];

            int bytesRead=0;
            while( (bytesRead = in.read(buf)) > 0){
                out.write(buf, 0, bytesRead);
            }
        }

        StructureImageExtractor sie = new StructureImageExtractor(out.toByteArray());
        spec.assertionConsumer.accept(sie.getChemical());
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> data(){
        List<Object[]> list = new ArrayList<>();

        list.add(new Object[]{"fluoxetineWikiTest", new TestSpec("moleculeTest/fluoxetine.png", c-> assertEquals("C17H18F3NO",c.getFormula()))});
        list.add(new Object[]{"reallyHardPeptideTest", new TestSpec("moleculeTest/peptideFragment.png", c-> {
            String key=Inchi.asStdInchi(c).getKey();
            //String form=c.getFormula();
            assertEquals("PLIFXMBNBJXWIM-MUGJNUQGSA-N",key);
        })});

        list.add(new Object[]{"lipitorWikiTest", new TestSpec("moleculeTest/lipitor.png", c-> {
            String key=Inchi.asStdInchi(c).getKey();
            //String form=c.getFormula();
            assertEquals("XUKUURHRXDUEBC-KAYWLYCHSA-N",key);
        })});

        list.add(new Object[]{"gleevecWikiTest", new TestSpec("moleculeTest/gleevec.png", c-> assertEquals("C29H31N7O",c.getFormula()))});
        list.add(new Object[]{"tylenolWikiTest", new TestSpec("moleculeTest/tylenol.png", c-> assertEquals("C8H9NO2",c.getFormula()))});

        list.add(new Object[]{"paxilWikiTest", new TestSpec("moleculeTest/paxil.png", c-> assertEquals("C19H20FNO3",c.getFormula()))});

        list.add(new Object[]{"complexStructure1Test", new TestSpec("moleculeTest/complex.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("[H][C@@]12CN(C[C@]1([H])[C@H]2NCc3ccc4cc(F)ccc4n3)c5ncc(cn5)C(=O)NO").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"fuzzyStructure1Test", new TestSpec("moleculeTest/fuzzy1.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("ClC(=O)c1ccc(Oc2ccc(cc2)C(Cl)=O)cc1").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"fuzzyStructure2Test", new TestSpec("moleculeTest/fuzzy2.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC(=C)C(=O)OCCOC(=O)c1ccc(C(=O)OCCCOC(=O)C=C)c(c1)C(=O)OCC(O)COc2ccc(Cc3ccc(OCC4CO4)cc3)cc2").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"zerosForOxygensAndSmallInnerBondTest", new TestSpec("moleculeTest/withZerosAsOxygens.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC1C2C=CC1C(C2C(=O)OCC(C)=C)C(=O)OCC(C)=C").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"subscriptImplicitAtomsCl3Test", new TestSpec("moleculeTest/withSubscriptForCl.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("ClC(Cl)(Cl)c1nc(nc(n1)C(Cl)(Cl)Cl)-c2ccc3OCOc3c2").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"subscriptImplicitAtomsH2Test", new TestSpec("moleculeTest/withSubscriptForH.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("COc1cccc(C(O)c2cc(F)ccc2-N)c1C").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"moleculeWithCloseNitrogensInRingTest", new TestSpec("moleculeTest/moleculeWithCloseNitrogensInRing.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("FC(F)(F)CNc1nc(Nc2ccc(cc2)N3CCOCC3)nc4ccsc14").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"moleculeWith2CarboxyShortHandsTest", new TestSpec("moleculeTest/carboxylicShorthandNotation.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("OC(=O)Cc1ccc(OCc2ccccc2C(O)=O)cc1").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"nCarbonChainTest", new TestSpec("moleculeTest/carbonChainShorthand.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CCCc1ccc(CCC)c2cc3c(-c4ccccc4)c5cc6c(CCC)ccc(CCC)c6cc5c(-c7ccccc7)c3cc12").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"serifFontTest", new TestSpec("moleculeTest/serifFont.png", c->{

            Chemical cReal=ChemicalBuilder.createFromMol("\n" +
                    "  CDK     01171907453D\n" +
                    "\n" +
                    " 36 40  0  0  0  0  0  0  0  0999 V2000\n" +
                    "   61.6266 -114.0482    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   41.2500  -71.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   39.0000 -156.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   41.9100 -237.9100    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   62.8606 -195.7677    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  155.0000 -272.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  115.5000 -249.2500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  239.7369 -269.2631    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  197.3154 -248.7265    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  271.9032  -71.9032    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  251.1223 -114.4840    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  197.3238  -59.3843    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  157.0000  -36.5000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  203.5000 -106.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  264.0000  -10.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  243.0968  -15.2258    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   49.0000 -299.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   69.0000 -294.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  107.5000 -202.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  301.0000 -262.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  294.0000 -241.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   19.0000 -241.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  294.0000  -69.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   19.0000  -69.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  243.0000 -292.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  272.5204 -237.0864    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "    7.0000  -49.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   70.0000  -16.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   73.2500  -39.7500    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "   74.0610 -269.9695    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  239.7773  -39.6384    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  116.4441  -59.7473    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  118.5000 -116.5000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  250.7303 -195.5577    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  192.0000 -193.0000    0.0000 N   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  274.0000 -154.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0\n" +
                    "  2 29  1  0  0  0  0 \n" +
                    " 26 34  1  0  0  0  0 \n" +
                    " 34 36  2  0  0  0  0 \n" +
                    " 32 33  1  0  0  0  0 \n" +
                    " 34 35  1  0  0  0  0 \n" +
                    "  7 19  2  0  0  0  0 \n" +
                    " 11 36  1  0  0  0  0 \n" +
                    "  4 30  2  0  0  0  0 \n" +
                    "  5 19  1  0  0  0  0 \n" +
                    " 15 16  1  0  0  0  0 \n" +
                    " 11 14  2  0  0  0  0 \n" +
                    " 18 30  1  0  0  0  0 \n" +
                    " 17 18  1  0  0  0  0 \n" +
                    "  8 25  1  0  0  0  0 \n" +
                    "  8 26  2  0  0  0  0 \n" +
                    "  9 35  1  0  0  0  0 \n" +
                    "  4 22  1  0  0  0  0 \n" +
                    " 16 31  1  0  0  0  0 \n" +
                    " 12 31  1  0  0  0  0 \n" +
                    "  2 24  1  0  0  0  0 \n" +
                    "  1 33  1  0  0  0  0 \n" +
                    " 28 29  1  0  0  0  0 \n" +
                    " 24 27  1  0  0  0  0 \n" +
                    " 10 23  1  0  0  0  0 \n" +
                    "  1  2  2  0  0  0  0 \n" +
                    "  1  3  1  0  0  0  0 \n" +
                    "  3  5  2  0  0  0  0 \n" +
                    "  4  5  1  0  0  0  0 \n" +
                    "  7 30  1  0  0  0  0 \n" +
                    " 12 13  2  0  0  0  0 \n" +
                    " 12 14  1  0  0  0  0 \n" +
                    " 29 32  2  0  0  0  0 \n" +
                    "  6  7  1  0  0  0  0 \n" +
                    "  6  9  2  0  0  0  0 \n" +
                    "  8  9  1  0  0  0  0 \n" +
                    " 21 26  1  0  0  0  0 \n" +
                    " 13 32  1  0  0  0  0 \n" +
                    " 10 31  2  0  0  0  0 \n" +
                    " 20 21  1  0  0  0  0 \n" +
                    " 10 11  1  0  0  0  0 \n" +
                    "M  END", Charset.defaultCharset()).build();
            //This smiles, which should be the same, doesn't seem to give the same formula. Don't know why
            //CCc1c(-C)c2cc3nc(nc4nc(cc5nc(cc1n2)c(-C)c5CC)c(-C)c4CC)c(-C)c3CC


            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"serifFont2Test", new TestSpec("moleculeTest/serifFont2.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("C=CC(C(c1ccc(OCCN2CCOCC2)cc1)c3ccc(OCCN4CCOCC4)cc3)c5ccccc5").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});
        list.add(new Object[]{"structureWithWeirdAngleBondToOtherOCRAtomTest", new TestSpec("moleculeTest/weirdAngleBetweenNitrogens.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("c1c(nn(c1-c2ccccc2)-c3cccc(c3)-n4c5ccc(cc5c6cc(ccc46)-c7ccccc7)-c8ccccc8)-c9ccccc9").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWhichIsAlmostARingAndHas2NonBondedOxygensVeryCloseTogether", new TestSpec("moleculeTest/almostRing.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC(=C)C(=O)OCCOCC(COC(=O)CCC(=O)OCCCCOC(=O)C=C)OC(=O)c1ccccc1C(=O)OCC(O)COc2ccc(cc2)C(C)(C)c3ccc(OCC4CO4)cc3").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithTightBondToNitrogens", new TestSpec("moleculeTest/tightBondsToNitrogens.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("C-c1cc(CN2C(=O)C3=C(CCCC3)C2=O)c(O)c(c1)-n4nc5ccccc5n4").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithLongLookingBondInAromaticRing", new TestSpec("moleculeTest/longLookingBondInAromaticRing.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("C(\\C=C\\c1ccc(cc1)N(c2ccccc2)c3ccc(\\C=N\\N(c4ccccc4)c5ccccc5)cc3)=C/c6ccccc6").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithAromaticSystemWhichSometimesChoosesWrongDoubleBond", new TestSpec("moleculeTest/aromaticSystemSometimesWrongDoubleBondChosen.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("c1ccc2cc3cc4cc5ccccc5cc4cc3cc2c1").build();

            Chemical c1=c.connectedComponentsAsStream()
                    .map(ct->Tuple.of(ct,ct.getAtomCount()).withVComparator())
                    .max(Comparator.naturalOrder())
                    .map(t->t.k())
                    .orElse(c);

            String form=c1.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithSmallLineForCl", new TestSpec("moleculeTest/smallLineCl.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("[H]n1c(Cl)nc2n(CCC3CC3)c(=O)n([H])c(=O)c12").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithOxygenOffCenterInRing", new TestSpec("moleculeTest/connectedOxygen.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("COc1cc2CN(C)c3c(ccc4cc5OCOc5cc34)-c2cc1OC").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithOxygenConnectedToBonds", new TestSpec("moleculeTest/connectedOxygen2.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("Oc1ccc-2c(Cc3c-2c4Cc5cc(O)ccc5-c4c6Cc7cc(O)ccc7-c36)c1").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithOxygenConnectedToBonds2", new TestSpec("moleculeTest/connectedOxygen3.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("O=C(Oc1ccc(OC(=O)C2CCC3C(C2)C(=O)OC3=O)cc1)C4CCC5C(C4)C(=O)OC5=O").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithExplicitCarbons", new TestSpec("moleculeTest/explicitCarbons.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC(=O)CC(C)=O").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithCarbonsThatAreSometimesMistakenForOxygens", new TestSpec("moleculeTest/carbonVsOxygen.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("ClC(Cl)(Cl)c1nc(nc(n1)C(Cl)(Cl)Cl)-c2ccc3OCOc3c2").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithBridgeHeadInsideRing", new TestSpec("moleculeTest/bridgeHeadMolecule.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC1C2C3OC3C1C(C2C(=O)OCC=C)C(=O)OCC(C)=C").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithOxygensThatAreSometimesMistakenForCarbons", new TestSpec("moleculeTest/carbonVsOxygen2.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CCOC(=O)CC1CCN(CC1)C(=O)CC2OC(c3cc(Cl)ccc3-n4cccc24)c5cccc6ccccc56").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithVeryCloseExplicitLinearAtoms", new TestSpec("moleculeTest/closeOCRShapesVeryExplicit.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CCC(C)(C)COC(=O)c1ccc(cc1)C(=O)OC").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithDoubelBondAngleSimilarToN", new TestSpec("moleculeTest/doubleBondSometimesSeenAsNAtom.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("OC(c1cccc2OCCOc12)c3cc(Cl)ccc3-n4cccc4\\C=C\\C(=O)OCc5ccccc5").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithCloseNitrogens", new TestSpec("moleculeTest/nitrogensSometimesBondedByMistake.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("OCCCCCNc1ncccc1C(=O)Nc2ccc(cc2)-n3nc(cc3C(F)(F)F)-c4cccnc4").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithVeryLongErroneousLineAndNoisyLine", new TestSpec("moleculeTest/longBadLineNoisyLine.png", c1->{

            Chemical cReal=ChemicalBuilder.createFromSmiles("O=C(Oc1ccc(OC(=O)c2ccc3C(=O)OC(=O)c3c2)cc1)c4ccc5C(=O)OC(=O)c5c4").build();

            Chemical c=c1.connectedComponentsAsStream()
                    .map(ct->Tuple.of(ct,ct.getAtomCount()).withVComparator())
                    .max(Comparator.naturalOrder())
                    .map(t->t.k())
                    .orElse(c1);

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithVeryCloseAromaticRings", new TestSpec("moleculeTest/veryCloseAromaticRings.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("COc1ccc(cc1)C2(c3ccccc3-c4ccccc24)c5ccc(OC)cc5").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithVeryShortSingleBondBetweenCarbons", new TestSpec("moleculeTest/verySmallSingleBondBetweenExplicitCarbons.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("C(C=Cc1ccc(cc1)N(c2ccccc2)c3ccc(cc3)-c4ccc(cc4)N(c5ccccc5)c6ccc(C=CC=Cc7ccccc7)cc6)=Cc8ccccc8").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"structureWithGapInSmallRing", new TestSpec("moleculeTest/moleculeWithGapInSmallRing.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("CC(=C)C(=O)OCCOC(=O)c1ccc(C(=O)OCCOC(=O)C=C)c(c1)C(=O)OCC(O)COc2ccc(cc2)C(C)(C)c3ccc(OCC4CO4)cc3").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        list.add(new Object[]{"aromaticRingSystemSometimesDoubleCounted", new TestSpec("moleculeTest/ringSystemProblem.png", c->{
            Chemical cReal=ChemicalBuilder.createFromSmiles("c1ccc(cc1)-c2c3c4ccc5c6cccc7cccc(c8ccc(c3c(-c9ccccc9)c%10ccccc2%10)c4c58)c67").build();

            String form=c.getFormula();
            assertEquals(cReal.getFormula(),form);
        } )});

        //This one needs work, it's an outlier
		/*
		list.add(new Object[]{"subscriptImplicitAtomsF3Test", new TestSpec("moleculeTest/withSubscriptForF.png", c->{
			Chemical cReal=ChemicalBuilder.createFromSmiles("FC(F)(F)C1(N=N1)c2ccc(CN3C(=O)C=CC3=O)cc2").build();

			String form=c.getFormula();
			assertEquals(cReal.getFormula(),form);
		} )});
		*/
        return list;
    }


}
