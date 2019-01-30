package tripod.molvec.algo;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import tripod.molvec.CachedSupplier;


class BranchNode{
	private final static HashSet<String> accept = CachedSupplier.of(()->{
		HashSet<String> keepers=new HashSet<String>();
		keepers.add("C");
		keepers.add("N");
		keepers.add("O");
		keepers.add("H");
		keepers.add("S");
		keepers.add("P");
		keepers.add("B");
		keepers.add("Br");
		keepers.add("Cl");
		keepers.add("F");
		
	    return keepers;
	}).get();
	
	String symbol = "C";
	int charge=0;
	boolean pseudo=false;
	boolean isRepeat=false;
	int rep=0;
	int orderToParent=1;
	private int thetaOffset=0;
	
	//1 is wedge, -1 is dash
	private int wedgeToParent=0;
	
	private boolean isTerminal =false;
	
	
	private boolean combineLinearly = false;
	
	private Tuple<BranchNode,Integer> ringBond = null; 
	private BranchNode childForCombine=null;
	
	private boolean isCombiner=false;
	
	List<BranchNode> children = new ArrayList<BranchNode>();
	
	
	Point2D suggestedPoint = new Point2D.Double(0, 0);
	
	public int getOrderToParent(){
		return this.orderToParent;
	}
	
	public BranchNode addRing(BranchNode to, int order){
		this.ringBond=Tuple.of(to,order);
		return this;
	}
	public Optional<Tuple<BranchNode,Integer>> getRingBond(){
		return Optional.ofNullable(ringBond);
	}
	
	public BranchNode thetaOffset(int o){
		this.thetaOffset=o;
		return this;
	}
	
	public BranchNode setOrderToParent(int o){
		this.orderToParent=o;
		return this;
	}
	
	public BranchNode setCharge(int c){
		this.charge=c;
		return this;
	}
	public int getCharge(){
		return this.charge;
	}
	
	public BranchNode generateCoordinates(){
		suggestedPoint = new Point2D.Double(0, 0);
		int totalChildren = children.size();
		
		double[] thetas=new double[]{-Math.PI/3,Math.PI/3,0,-2*Math.PI/3,2*Math.PI/3};
		
		for(int i=0;i<totalChildren;i++){
			BranchNode child = children.get(i);
			child.generateCoordinates();
			AffineTransform at = new AffineTransform();
			
			int ti = i+thetaOffset;
			double ntheta = ti<thetas.length?thetas[ti]:0;
			at.rotate(ntheta);
			at.translate(1, 0);
			
			
			child.applyTransform(at);
			
		}
		return this;
		
	}
	
	public BranchNode applyTransform(AffineTransform at){
		
		suggestedPoint=at.transform(suggestedPoint, null);
		for(BranchNode c:this.getChildren()){
			c.applyTransform(at);
		}
		return this;
	}
	
	public String getSymbol(){
		if(this.isPseudoNode()){
			return "?";
		}else if(this.isRepeatNode()){
			return "'" + this.rep + "'";
		}
		return this.symbol;
	}
	
	public boolean shouldComineLinearly(){
		if(combineLinearly){
			return true;
		}
		return false;
	}
	
	public boolean hasChildren(){
		return !children.isEmpty();
	}
	public List<BranchNode> getChildren(){
		return this.children;
	}
	public boolean isRealNode(){
		return !pseudo && !isRepeat;
	}
	public boolean isPseudoNode(){
		return pseudo;
	}
	public boolean isRepeatNode(){
		return this.isRepeat;
	}
	public int getRepeat(){
		return this.rep;
	}
	public BranchNode setRepeat(int r){
		this.isRepeat=true;
		this.rep=r;
		return this;
	}
	
	public BranchNode getNodeForLinearCombine(){
		if(childForCombine!=null){
			return childForCombine;
		}
		return this;
	}
	
	public BranchNode setCombiningNode(BranchNode bn){
		this.childForCombine=bn;
		return this;
	}
	
	public BranchNode flagForCombining(){
		this.isCombiner=true;
		return this;
	}
	
	public BranchNode addChild(BranchNode bn){
		BranchNode ret=_addChild(bn);
		return ret;
	}
	//Always add to the left
	//
	private BranchNode _addChild(BranchNode bn){
		BranchNode nToAddTo= this.getNodeForLinearCombine();
		
		if(bn.isCombiner){
			this.setCombiningNode(bn);
		}
		if(bn.isRepeatNode()){
			int rep=bn.getRepeat();
			if(this.shouldComineLinearly()){
				BranchNode parent=nToAddTo;
				for(int i=1;i<rep;i++){
					BranchNode bnew=new BranchNode(nToAddTo.symbol).thetaOffset(i%2);
					parent.addChild(bnew);
					parent=bnew;
				}
			}else{
				nToAddTo.setPseudoNode(true);
				for(int i=0;i<rep;i++){
					nToAddTo.addChild(new BranchNode(nToAddTo.symbol));
				}
			}
						
			if(bn.hasChildren()){
				//This usually means the text is inverted
				if(bn.getChildren().size()!=1){
					throw new IllegalStateException("Node:[" + bn.toString() + "] needs to be added, but has ambiguous meaning");
				}
				
				BranchNode nchild=bn.getChildren().get(0);
				return nchild.addChild(this);
			}
		}else if(bn.isPseudoNode()){
			nToAddTo.children.addAll(bn.children);
		}else{
			nToAddTo.children.add(bn);
		}
		return this;
	}
	
	public BranchNode setPseudoNode(boolean p){
		this.pseudo=p;
		return this;
	}
	
	private BranchNode rightBranch=this;
	private BranchNode leftBranch=this;
	
	public BranchNode getLeftBranchNode(){
		return rightBranch;
	}
	
	public BranchNode getRightBranchNode(){
		return leftBranch;
	}
	
	public BranchNode setRightBranchNode(BranchNode bn){
		this.rightBranch=bn;
		return this;
	}
	public BranchNode setLeftBranchNode(BranchNode bn){
		this.leftBranch=bn;
		return this;
	}
	
	public boolean canBeChain(){
		return this.leftBranch!=this.rightBranch;
	}
	
	public BranchNode(String s){
		this.symbol=s;
		if(this.symbol.equals("C")){
			combineLinearly=true;
		}
	}
	
	public BranchNode setShouldCombineLinearly(boolean t){
		this.combineLinearly=t;
		return this;
	}
	
	public String toString(){
		String bondToParent="-";
		if(this.getOrderToParent()==2){
			bondToParent="=";
		}else if(this.getOrderToParent()==3){
			bondToParent="#";
		}
		if(this.hasChildren()){
			return bondToParent + this.getSymbol() + "(" + this.children.stream()
			                               .map(b->b.toString())
					                       .collect(Collectors.joining(",")) + ")";
		}
		return bondToParent+this.getSymbol();
	}
	
	private void forEachBranchNode(BiConsumer<BranchNode,BranchNode> cons, BranchNode parent){
		cons.accept(parent, this);
		this.getChildren()
		.stream()
		.collect(Collectors.toList())
		.forEach(child->{
			child.forEachBranchNode(cons,this);
		});
	}
	
	public void forEachBranchNode(BiConsumer<BranchNode,BranchNode> parentAndChild){
		forEachBranchNode(parentAndChild,null);
	}

	private static BranchNode interpretOCRStringAsAtom(String s, boolean tokenOnly){
		
		//first step is to see if this is an aliphatic chain
		if(s.matches("([cC]H[2]*)+")){
			int c=s.split("H[2]*").length;
			
			BranchNode full = new BranchNode("C");
			BranchNode parent=full;
			for(int i=1;i<c;i++){
				BranchNode nn=new BranchNode("C").thetaOffset(i%2);
				parent.addChild(nn);
				parent=nn;
			}
			full.setRightBranchNode(parent);
			return full;
		}else if(s.equalsIgnoreCase("NHCO")){
			
			BranchNode full = new BranchNode("N");
			
			BranchNode carb = new BranchNode("C").thetaOffset(1).addChild(new BranchNode("O").setOrderToParent(2));
			full.addChild(carb);
			full.setRightBranchNode(carb);
			
			return full;
		}
		
		if(s.equalsIgnoreCase("CH3CONH")){
			return interpretOCRStringAsAtom("N").addChild(interpretOCRStringAsAtom("CO").addChild(new BranchNode("C")));
		}else if((  s.equalsIgnoreCase("CO2H")
				|| s.equalsIgnoreCase("CO2")
				|| s.equalsIgnoreCase("COOH")
				|| s.equalsIgnoreCase("HOOC")
				|| s.equalsIgnoreCase("HO2C")
				|| s.equalsIgnoreCase("OOC")
				|| s.equalsIgnoreCase("COO")

				|| s.equalsIgnoreCase("C02H")
				|| s.equalsIgnoreCase("C02")
				|| s.equalsIgnoreCase("C00H")
				|| s.equalsIgnoreCase("H00C")
				|| s.equalsIgnoreCase("H02C")
				|| s.equalsIgnoreCase("O00")
				|| s.equalsIgnoreCase("C00")
				)){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if((       s.equalsIgnoreCase("CN")
				|| s.equalsIgnoreCase("NC"))){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("N").setOrderToParent(3));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if((s.equalsIgnoreCase("SO3")
				|| s.equalsIgnoreCase("O3S"))){
			BranchNode bn = new BranchNode("S");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if((s.equalsIgnoreCase("NO2")
				|| s.equalsIgnoreCase("O2N"))){
			BranchNode bn = new BranchNode("N").setCharge(1);
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(1).setCharge(-1));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if(s.equalsIgnoreCase("CH3")){
			BranchNode bn = new BranchNode("C").setShouldCombineLinearly(false).setTerminal(true);
			return bn;
		}else if(s.equals("Me")||s.equals("Mc")||s.equals("MC")){
			BranchNode bn = new BranchNode("C").setShouldCombineLinearly(false).setTerminal(true);
			return bn;
		}else if(s.equals("Et")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("C"));
			return bn;
		}else if(s.equals("Pr")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("C").addChild(new BranchNode("C")));
			return bn;
		}else if(s.equals("t")){
			BranchNode bn = new BranchNode("I");
			return bn;
		}else if(s.equals("NCH3") || s.equals("NcH3") || s.equals("NHcH3") || s.equals("NHCH3")){
			BranchNode bn = new BranchNode("N");
			bn.addChild(new BranchNode("C"));
			return bn;
		}else if((s.equalsIgnoreCase("SO2")
				|| s.equalsIgnoreCase("S02")
				|| s.equalsIgnoreCase("O2S")
				|| s.equalsIgnoreCase("02S"))){
			BranchNode bn = new BranchNode("S");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if(s.equalsIgnoreCase("Ms") || s.equalsIgnoreCase("M8")){
			BranchNode bn = new BranchNode("S");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("C").setOrderToParent(1));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if(s.equalsIgnoreCase("Boc")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("O").setOrderToParent(1)
					.addChild(new BranchNode("C")
							.addChild(new BranchNode("C"))
							.addChild(new BranchNode("C"))
							.addChild(new BranchNode("C"))
							));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if(s.equalsIgnoreCase("Boc2N")){
			BranchNode b1=interpretOCRStringAsAtom("Boc");
			BranchNode b2=interpretOCRStringAsAtom("Boc");
			BranchNode bn = new BranchNode("N");
			bn.addChild(b1);
			bn.addChild(b2);
			return bn;
		}else if(s.equalsIgnoreCase("N3")){
			BranchNode bn = new BranchNode("N").setCharge(-1);
			bn.addChild(new BranchNode("N").setOrderToParent(1).setCharge(1)
					.addChild(new BranchNode("N").setOrderToParent(3)));
			//bn.addChild(new BranchNode("O").setOrderToParent(1).flagForCombining());
			return bn;
		}else if(s.equalsIgnoreCase("CO")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("O").setOrderToParent(2));
			return bn;
		}else if(s.equalsIgnoreCase("OH")){
			return new BranchNode("O").setTerminal(true);
		}else if(s.equalsIgnoreCase("CH2O")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("O").setOrderToParent(1));
			return bn;
		}else if(s.equalsIgnoreCase("Ac")){
			BranchNode bn = new BranchNode("C").setOrderToParent(1)
										   .addChild(new BranchNode("C").setOrderToParent(1))
					                       .addChild(new BranchNode("O").setOrderToParent(2));
			
			return bn;
		}else if(s.equalsIgnoreCase("AcO")){
			BranchNode bn = new BranchNode("O");
			BranchNode ac=interpretOCRStringAsAtom("Ac");
			bn.addChild(ac);

			return bn;
		}else if(s.equalsIgnoreCase("EtO2C")){
			
			BranchNode co2=interpretOCRStringAsAtom("CO2");
			BranchNode et=interpretOCRStringAsAtom("Et");
			
			co2.addChild(et);
			return co2;
		}else if(s.equalsIgnoreCase("Bn") || s.equalsIgnoreCase("Bt1")){
			
			BranchNode carb=interpretOCRStringAsAtom("C");
			BranchNode ben=interpretOCRStringAsAtom("Ph");
			
			
			carb.addChild(ben);
			return carb;
		}else if(s.equalsIgnoreCase("CBZ") || s.equalsIgnoreCase("C6Z")){
			System.out.println("Found cbz");
			BranchNode carb=interpretOCRStringAsAtom("C").addChild(new BranchNode("O").setOrderToParent(2));
			BranchNode ox=new BranchNode("O").setOrderToParent(1);
			carb.addChild(ox);
			BranchNode ben=interpretOCRStringAsAtom("Bn");
			ox.addChild(ben);
			
			return carb;
		}else if(s.equals("t-Bu") || s.equals("tBu") || s.equals("t-Bo") || s.equals("tBo")){
			BranchNode bn = new BranchNode("C");
			bn.addChild(new BranchNode("C"));
			bn.addChild(new BranchNode("C"));
			bn.addChild(new BranchNode("C"));
			return bn;
		}else if(s.equals("n-Bu") || s.equals("nBu") || s.equals("n-Bo") || s.equals("nBo")){
			return interpretOCRStringAsAtom("C4H9");
		}else if(s.equalsIgnoreCase("CO2Cys") || s.equalsIgnoreCase("CO2Cy8")){
			
			BranchNode co2=interpretOCRStringAsAtom("CO2");
			BranchNode cys=interpretOCRStringAsAtom("Cys");
			
			BranchNode combine=cys.getNodeForLinearCombine();
			
			co2.addChild(cys);
			co2.setCombiningNode(combine);
			return co2;
		}else if(s.equalsIgnoreCase("Cys")){
			BranchNode bn = new BranchNode("N");
			BranchNode carbonyl=new BranchNode("C").addChild(new BranchNode("O").setOrderToParent(2));
			bn.addChild(new BranchNode("C").setOrderToParent(1)
										   .flagForCombining()
					                       .addChild(new BranchNode("C").thetaOffset(1).setWedgeToParent(1).addChild(new BranchNode("S")))
					                       .addChild(carbonyl)
					);
			
			bn.setCombiningNode(carbonyl);
			return bn;
		}else if(s.equalsIgnoreCase("Ph") || s.equals("Pb") || s.equals("pb")){
			BranchNode bn = new BranchNode("C").thetaOffset(1);
			
			bn.addChild(new BranchNode("C").setOrderToParent(2)
					    			       .addChild(new BranchNode("C").setOrderToParent(1)
					    			    		   .addChild(new BranchNode("C").setOrderToParent(2)
					    			    				   .addChild(new BranchNode("C").setOrderToParent(1).addChild(new BranchNode("C").setOrderToParent(2).addRing(bn, 1)))
					    			    		   )
					    			    	)
					);
			
			
			return bn;
		}else if(s.equals("[p-tol]")){
			BranchNode bn = new BranchNode("C").thetaOffset(1);
			
			bn.addChild(new BranchNode("C").setOrderToParent(2) //ortho
					    			       .addChild(new BranchNode("C").setOrderToParent(1) //meta
					    			    		   .addChild(new BranchNode("C").setOrderToParent(2) //para
					    			    				   .addChild(new BranchNode("C").setOrderToParent(1) //meta
					    			    						   .addChild(new BranchNode("C").setOrderToParent(2).addRing(bn, 1))) //ortho
					    			    				   .addChild(new BranchNode("C"))
					    			    		   )
					    			    	)
					);
			
			
			return bn;
		}else if(s.equals("Ts")){
			return interpretOCRStringAsAtom("SO2[p-tol]");
		}else if(s.equalsIgnoreCase("PMBN")){
			BranchNode bno = new BranchNode("N").thetaOffset(1);
			BranchNode ml = new BranchNode("C");
			bno.addChild(ml);
			
			BranchNode bn = new BranchNode("C").thetaOffset(1);
			
			bn.addChild(new BranchNode("C").setOrderToParent(2)
					    			       .addChild(new BranchNode("C").setOrderToParent(1)
					    			    		   .addChild(new BranchNode("C").setOrderToParent(2)
					    			    				   .addChild(new BranchNode("C").setOrderToParent(1).addChild(new BranchNode("C").setOrderToParent(2).addRing(bn, 1)))
					    			    				   .addChild(new BranchNode("O").addChild(new BranchNode("C")))
					    			    		   )
					    			    	)
					);
			ml.addChild(bn);
			
			
			return bno;
		}else if(s.equalsIgnoreCase("CH3O") || s.equalsIgnoreCase("H3CO")){
			return interpretOCRStringAsAtom("OCH3");
		}else if(s.equalsIgnoreCase("OCH3")){
			return interpretOCRStringAsAtom("O").addChild(interpretOCRStringAsAtom("C"));
		}else if(s.equalsIgnoreCase("EtHN")){
			return interpretOCRStringAsAtom("NHEt");
		}else if(s.equalsIgnoreCase("OHC")){
			return interpretOCRStringAsAtom("COH");
		}else if(s.matches("II*O")){ // usually from dash bonds
			return interpretOCRStringAsAtom("O");
		}else if(s.matches("N[lI][lI]*")){ // usually from dash bonds
			return interpretOCRStringAsAtom("N");
		}else if(s.equalsIgnoreCase("S+")){
			BranchNode bn = new BranchNode("S");
			bn.setCharge(1);
			return bn;
		}else if(s.equalsIgnoreCase("N+") || s.equalsIgnoreCase("+N")){
			BranchNode bn = new BranchNode("N");
			bn.setCharge(1);
			return bn;
		}else if(s.equalsIgnoreCase("BnO")){
			return interpretOCRStringAsAtom("OBn");
		}else if(s.equalsIgnoreCase("MeO") || s.equalsIgnoreCase("McO")){
			return interpretOCRStringAsAtom("OMe");
		}else if(s.equals("BnN") || s.equals("Bt1N")){
			return interpretOCRStringAsAtom("NBn");
		}else if(s.equals("MeN") || s.equals("McN")){
			return interpretOCRStringAsAtom("NMe");
		}else if(s.equals("MeS") || s.equals("McS")){
			return interpretOCRStringAsAtom("SMe");
		}else if(s.equalsIgnoreCase("EtO") ){
			return interpretOCRStringAsAtom("OEt");
		}else if(s.equalsIgnoreCase("F3CO") ){
			return interpretOCRStringAsAtom("OCF3");
		}else if(s.equalsIgnoreCase("EtOOC") ){
			return interpretOCRStringAsAtom("COOEt");
		}else if(s.equalsIgnoreCase("H3CS") ){
			return interpretOCRStringAsAtom("SCH3");
		}else if(s.equalsIgnoreCase("PH3C") ){
			return interpretOCRStringAsAtom("CPH3");
		}else if(s.equalsIgnoreCase("HOH2C") ){
			return interpretOCRStringAsAtom("CH2OH");
		}
//		else if(s.equalsIgnoreCase("CH3CONH") ){
//			return interpretOCRStringAsAtom("NHOCCH3");
//		}
		else if(s.equalsIgnoreCase("SOCH3") ){
			BranchNode bn = new BranchNode("S");
			bn.setCharge(1);
			bn.addChild(new BranchNode("O").setCharge(-1));
			bn.addChild(interpretOCRStringAsAtom("CH3"));			
			return bn;
		}else if(s.matches("[(].*[)][0-9][0-9]*")){
			Pattern p = Pattern.compile("[(](.*)[)]([0-9][0-9]*)");
			Matcher m=p.matcher(s);
			if(m.find()){
				String s1=m.group(1);
				String s2=m.group(2);
				
				int n=Integer.parseInt(s2);
				
				BranchNode ps = new BranchNode("?").setPseudoNode(true);
				
				
				if(n<10){
					for(int i=0;i<n;i++){
						BranchNode bnnew=interpretOCRStringAsAtom(s1);
						if(bnnew!=null && bnnew.isRealNode()){
							ps.addChild(bnnew);
						}else{
							ps=null;
							break;
						}
					}
				}
				if(ps!=null)return ps;
				
				
				//System.out.println("Got:" + s1 + "," + s2 + " from " + s);
			}
			 
		}else if(s.matches("[(].*[)][0-9][0-9]*N")){
			Pattern p = Pattern.compile("[(](.*)[)]([0-9][0-9]*)N");
			Matcher m=p.matcher(s);
			if(m.find()){
				return interpretOCRStringAsAtom("N(" + m.group(1) + ")" + m.group(2));
			}
		}
		
		
		
		//TODO:
		
		// Et
		// Ph
		
		if(accept.contains(s)){
			BranchNode bn=new BranchNode(s);
			if(s.equals("F") || s.equals("Br") || s.equals("Cl")){
				bn=bn.setTerminal(true);
			}
			return bn;
		}else if(accept.contains(s.toUpperCase())){
			return new BranchNode(s.toUpperCase());
		}
		if(s.contains("Ct")){
			return interpretOCRStringAsAtom(s.replaceAll("C[tT]", "Cl"),tokenOnly);
		}
		if(s.contains("Htt")){
			return interpretOCRStringAsAtom(s.replace("Htt", "H11"),tokenOnly);
		}
		if(s.contains("Ht")){
			return interpretOCRStringAsAtom(s.replace("Ht", "H1"),tokenOnly);
		}
		if(s.contains("H")){
			return interpretOCRStringAsAtom(s.replaceAll("H[1-9][0-9]*", "").replace("H", ""),tokenOnly);
		}
		if(s.contains("I")){
			return interpretOCRStringAsAtom(s.replace("I", "l"),tokenOnly);
		}

		if(s.contains("t1")){
			return interpretOCRStringAsAtom(s.replace("t1", "n"),tokenOnly);
		}
		if(s.contains("1")){
			return interpretOCRStringAsAtom(s.replace("1", "l"),tokenOnly);
		}
		if(s.contains("c")){
			return interpretOCRStringAsAtom(s.replace("c", "C"),tokenOnly);
		}
		if(s.equals("0")){
			return interpretOCRStringAsAtom(s.replace("0", "O"),tokenOnly);
		}
		
		
		try{
			int r=Integer.parseInt(s);
			if(r>0){
				BranchNode repNode=new BranchNode("?");
				repNode.setRepeat(r);
				return repNode;
			}
		}catch(Exception e){
			
		}
		if(tokenOnly){
			return null;
		}
		
		
		//start breaking up
		if(s.length()>1){
			BranchNode parent=null;
			String rem=null;
			for(int i=s.length()-1;i>=1;i--){
				String substr=s.substring(0, i);
				BranchNode bn1=interpretOCRStringAsAtom(substr,true);
				if(bn1!=null){
					parent=bn1;
					//System.out.println(bn1.toString());
					rem=s.substring(i);
					BranchNode child=interpretOCRStringAsAtom(rem);
					if(child!=null){
						BranchNode bnFinal= parent.addChild(child);
						//System.out.println("F:" + bnFinal.toString());
						return bnFinal;
					}
					//break;
				}
			}
			if(parent!=null){
				
			}
		}
		
		return null;		
	}
	
	public BranchNode removeHydrogens(){
		BranchNode _this=this;
		this.forEachBranchNode((p,c)->{
			if(p!=null){
				if(c.getSymbol().equals("H")){
					p.children.remove(c);
				}
			}
		});
		
		return this;
	}
	public BranchNode setWedgeToParent(int i) {
		this.wedgeToParent=i;
		return this;
	}
	public int getWedgeType(){
		return this.wedgeToParent;
	}

	private static BranchNode interpretOCRStringAsAtom(String s){
		try{
			return interpretOCRStringAsAtom(s,false);
		}catch(Exception e){
			return null;
		}
	}
	
	public static BranchNode interpretOCRStringAsAtom2(String s){
		try{
			BranchNode bn= interpretOCRStringAsAtom(s);
			if(bn!=null)bn.removeHydrogens();
			return bn;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	

	public boolean isTerminal() {
		return isTerminal;
	}
	public BranchNode setTerminal(boolean t){
		this.isTerminal=t;
		return this;
	}
	
}