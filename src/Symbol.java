/**
 *����C���԰汾�в���ȫ�ֱ���sym���洢�����룬����ȫ�ֱ���id��num����������ֵ��
 *���汾�а������߷�װ������ͬʱҲ�ѷ������Գ�������ʽ��װ������
 *
 */
public class Symbol {
	// ���������
	public static final int nul 		= 0;
	public static final int ident 		= 1;
	public static final int number 		= 2;
	public static final int plus 		= 3;
	public static final int minus 		= 4;
	public static final int times 		= 5;
	public static final int slash 		= 6;
	public static final int oddsym 		= 7;
	public static final int eql 		= 8;
	public static final int neq 		= 9;
	public static final int lss 		= 10;
	public static final int leq 		= 11;
	public static final int gtr 		= 12;
	public static final int geq 		= 13;
	public static final int lparen 		= 14;
	public static final int rparen 		= 15;
	public static final int comma 		= 16;
	public static final int semicolon 	= 17;
	public static final int period 		= 18;
	public static final int becomes 	= 19;
	public static final int beginsym 	= 20;
	public static final int endsym 		= 21;
	public static final int ifsym 		= 22;
	public static final int thensym 	= 23;
	public static final int whilesym 	= 24;
	public static final int writesym 	= 25;
	public static final int readsym 	= 26;
	public static final int dosym 		= 27;
	public static final int callsym 	= 28;
	public static final int constsym 	= 29;
	public static final int varsym 		= 30;
	public static final int procsym 	= 31;

	// ������ĸ���
	public static final int symnum = 32;
	
	/**
	 * ������
	 */
	public int symtype;

	/**
	 * ��ʶ�����֣������������Ǳ�ʶ���Ļ���
	 */
	public String id;

	/**
	 * ��ֵ��С�����������������ֵĻ���
	 */
	public int num;

	/**
	 * ��������ض�������ķ���
	 * @param stype ������
	 */
	Symbol(int stype) {
		symtype = stype;
		id = "";
		num = 0;
	}
}
