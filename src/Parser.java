import java.util.BitSet;

/**
 *�����﷨������������PL/0������������Ҫ�Ĳ��֣����﷨�����Ĺ�����Ƕ�����﷨�������Ŀ��������ɡ�
 */
public class Parser {
	private Scanner lex;					// �Դʷ�������������
	private Table table;					// �Է��ű������
	private Interpreter interp;				// ��Ŀ�����������������
	
	// ��ʾ������ʼ�ķ��ż��ϡ���ʾ��俪ʼ�ķ��ż��ϡ���ʾ���ӿ�ʼ�ķ��ż���
	// ʵ����������������������ӵ�FIRST����
	private BitSet declbegsys, statbegsys, facbegsys;
	
	/**
	 * ��ǰ���ŵķ����룬��nextsym()����
	 * @see #nextsym()
	 */
	private int symtype;
	
	/**
	 * ��ǰ���ţ���nextsym()����
	 * @see #nextsym()
	 */
	private Symbol sym;
	
	/**
	 * ��ǰ������Ķ�ջ֡��С������˵���ݴ�С��data size��
	 */
	private int dx = 0;
	
	/**
	 * ���첢��ʼ���﷨�����������������C���԰汾��init()������һ���ִ���
	 * @param l �������Ĵʷ�������
	 * @param t �������ķ��ű�
	 * @param i ��������Ŀ�����������
	 */
	public Parser(Scanner l, Table t, Interpreter i) {
		lex = l;
		table = t;
		interp = i;
		
		// ����������ʼ���ż�
		declbegsys = new BitSet(Symbol.symnum);
		declbegsys.set(Symbol.constsym);
		declbegsys.set(Symbol.varsym);
		declbegsys.set(Symbol.procsym);

		// ������俪ʼ���ż�
		statbegsys = new BitSet(Symbol.symnum);
		statbegsys.set(Symbol.beginsym);
		statbegsys.set(Symbol.callsym);
		statbegsys.set(Symbol.ifsym);
		statbegsys.set(Symbol.whilesym);

		// �������ӿ�ʼ���ż�
		facbegsys = new BitSet(Symbol.symnum);
		facbegsys.set(Symbol.ident);
		facbegsys.set(Symbol.number);
		facbegsys.set(Symbol.lparen);

	}
	
	/**
	 * �����﷨�������̣���ǰ�����ȵ���һ��nextsym()
	 * @see #nextsym()
	 */
	public void parse() {
		BitSet nxtlev = new BitSet(Symbol.symnum);
		nxtlev.or(declbegsys);
		nxtlev.or(statbegsys);
		nxtlev.set(Symbol.period);
		block(0, nxtlev);
		
		if (symtype != Symbol.period)
			Err.report(9);
	}
	
	/**
	 * �����һ���﷨���ţ�����ֻ�Ǽ򵥵���һ��getsym()
	 */
	public void nextsym() {
		lex.getsym();
		sym =lex.sym;
		symtype = sym.symtype;
	}
	
	/**
	 * ���Ե�ǰ�����Ƿ�Ϸ�
	 * 
	 * @param s1 ������Ҫ�ķ���
	 * @param s2 �������������Ҫ�ģ�����Ҫһ�������õļ���
	 * @param errcode �����
	 */
	void test(BitSet s1, BitSet s2, int errcode) {
		// ��ĳһ���֣���һ����䣬һ�����ʽ����Ҫ����ʱʱ����ϣ����һ����������ĳ����
		//���ò��ֵĺ�����ţ���test���������⣬���Ҹ��𵱼�ⲻͨ��ʱ�Ĳ��ȴ�ʩ����
		// ������Ҫ���ʱָ����ǰ��Ҫ�ķ��ż��ϺͲ����õļ��ϣ���֮ǰδ��ɲ��ֵĺ����
		// �ţ����Լ���ⲻͨ��ʱ�Ĵ���š�
		if (!s1.get(symtype)) {
			Err.report(errcode);
			// ����ⲻͨ��ʱ����ͣ��ȡ���ţ�ֱ����������Ҫ�ļ��ϻ򲹾ȵļ���
			while (!s1.get(symtype) && !s2.get(symtype))
				nextsym();
		}
	}
	
	/**
	 * ����<�ֳ���>
	 * 
	 * @param lev ��ǰ�ֳ������ڲ�
	 * @param fsys ��ǰģ�������ż�
	 */
	public void block(int lev, BitSet fsys) {
		// <�ֳ���> := [<����˵������>][<����˵������>][<����˵������>]<���>
		
		int dx0, tx0, cx0;				// ������ʼdx��tx��cx
		BitSet nxtlev = new BitSet(Symbol.symnum);
		
		dx0 = dx;						// ��¼����֮ǰ�����������Ա�ָ���
		dx = 3;
		tx0 = table.tx;					// ��¼�������ֵĳ�ʼλ�ã��Ա�ָ���
		table.get(table.tx).adr = interp.cx;
		
		interp.gen(Instr.jmp, 0, 0);
		
		if (lev > PL0.levmax)
			Err.report(32);
		
		// ����<˵������>
		do {
			// <����˵������>
			if (symtype == Symbol.constsym) {
				nextsym();
				// the original do...while(sym == ident) is problematic, thanks to calculous
				// do
				constdeclaration(lev);
				while (symtype == Symbol.comma) {
					nextsym();
					constdeclaration(lev);
				}
				
				if (symtype == Symbol.semicolon)
					nextsym();
				else
					Err.report(5);				// ©���˶��Ż��߷ֺ�
				// } while (sym == ident);
			}
			
			// <����˵������>
			if (symtype == Symbol.varsym) {
				nextsym();
				/* the original do...while(sym == ident) is problematic, thanks to calculous */
				/* do {  */
				vardeclaration(lev);
				while (symtype == Symbol.comma)
				{
					nextsym();
					vardeclaration(lev);
				}
				
				if (symtype == Symbol.semicolon)
					nextsym();
				else
					Err.report(5);				// ©���˶��Ż��߷ֺ�
				/* } while (sym == ident);  */
			}
			
			// <����˵������>
			while (symtype == Symbol.procsym) {
				nextsym();
				if (symtype == Symbol.ident) {
					table.enter(sym, Table.Item.procedur, lev, dx);
					nextsym();
				} else { 
					Err.report(4);				// procedure��ӦΪ��ʶ��
				}

				if (symtype == Symbol.semicolon)
					nextsym();
				else
					Err.report(5);				// ©���˷ֺ�
				
				nxtlev = (BitSet) fsys.clone();
				nxtlev.set(Symbol.semicolon);
				block(lev+1, nxtlev);
				
				if (symtype == Symbol.semicolon) {
					nextsym();
					nxtlev = (BitSet) statbegsys.clone();
					nxtlev.set(Symbol.ident);
					nxtlev.set(Symbol.procsym);
					test(nxtlev, fsys, 6);
				} else { 
					Err.report(5);				// ©���˷ֺ�
				}
			}
			
			nxtlev = (BitSet) statbegsys.clone(); 
			nxtlev.set(Symbol.ident);
			test(nxtlev, declbegsys, 7);
		} while (declbegsys.get(symtype));		// ֱ��û����������
		
		// ��ʼ���ɵ�ǰ���̴���
		Table.Item item = table.get(tx0);
		interp.code[item.adr].a = interp.cx;
		item.adr = interp.cx;					// ��ǰ���̴����ַ
		item.size = dx;							// ����������ÿ����һ�����������dx����1��
												// ���������Ѿ�������dx���ǵ�ǰ���̵Ķ�ջ֡��С
		cx0 = interp.cx;
		interp.gen(Instr.inte, 0, dx);			// ���ɷ����ڴ����
		
		table.debugTable(tx0);
			
		// ����<���>
		nxtlev = (BitSet) fsys.clone();		// ÿ��������ż��Ͷ������ϲ������ż��ͣ��Ա㲹��
		nxtlev.set(Symbol.semicolon);		// ���������Ϊ�ֺŻ�end
		nxtlev.set(Symbol.endsym);
		statement(nxtlev, lev);
		interp.gen(Instr.opr, 0, 0);		// ÿ�����̳��ڶ�Ҫʹ�õ��ͷ����ݶ�ָ��
		
		nxtlev = new BitSet(Symbol.symnum);	// �ֳ���û�в��ȼ���
		test(fsys, nxtlev, 8);				// �����������ȷ��
		
		interp.listcode(cx0);
		
		dx = dx0;							// �ָ���ջ֡������
		table.tx = tx0;						// �ظ����ֱ�λ��
	}

	/**
	 * ����<����˵������>
	 * @param lev ��ǰ���ڵĲ��
	 */
	void constdeclaration(int lev) {
		String id;
		if (symtype == Symbol.ident) {
			id = sym.id;
			nextsym();
			if (symtype == Symbol.eql || symtype == Symbol.becomes) {
				if (symtype == Symbol.becomes) 
					Err.report(1);			// �� = д���� :=
				nextsym();
				if (symtype == Symbol.number) {
					sym.id = id;
					table.enter(sym, Table.Item.constant, lev, dx);
					nextsym();
				} else {
					Err.report(2);			// ����˵�� = ��Ӧ������
				}
			} else {
				Err.report(3);				// ����˵����ʶ��Ӧ�� =
			}
		} else {
			Err.report(4);					// const ��Ӧ�Ǳ�ʶ��
		}
	}

	/**
	 * ����<����˵������>
	 * @param lev ��ǰ���
	 */
	void vardeclaration(int lev) {
		if (symtype == Symbol.ident) {
			// ��д���ֱ��ı��ջ֡������
			table.enter(sym, Table.Item.variable, lev, dx);
			dx ++;
			nextsym();
		} else {
			Err.report(4);					// var ��Ӧ�Ǳ�ʶ
		}
	}

	/**
	 * ����<���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	void statement(BitSet fsys, int lev) {
		BitSet nxtlev;
		// Wirth �� PL/0 ������ʹ��һϵ�е�if...else...������
		// �������������Ϊ�����д���ܹ���������ؿ�����������Ĵ����߼�
		switch (symtype) {
		case Symbol.ident:
			parseAssignStatement(fsys, lev);
			break;
		case Symbol.readsym:
			parseReadStatement(fsys, lev);
			break;
		case Symbol.writesym:
			parseWriteStatement(fsys, lev);
			break;
		case Symbol.callsym:
			parseCallStatement(fsys, lev);
			break;
		case Symbol.ifsym:
			parseIfStatement(fsys, lev);
			break;
		case Symbol.beginsym:
			parseBeginStatement(fsys, lev);
			break;
		case Symbol.whilesym:
			parseWhileStatement(fsys, lev);
			break;
		default:
			nxtlev = new BitSet(Symbol.symnum);
			test(fsys, nxtlev, 19);
			break;
		}
	}

	/**
	 * ����<����ѭ�����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseWhileStatement(BitSet fsys, int lev) {
		int cx1, cx2;
		BitSet nxtlev;
		
		cx1 = interp.cx;						// �����ж�����������λ��
		nextsym();
		nxtlev = (BitSet) fsys.clone();
		nxtlev.set(Symbol.dosym);				// �������Ϊdo
		condition(nxtlev, lev);					// ����<����>
		cx2 = interp.cx;						// ����ѭ����Ľ�������һ��λ��
		interp.gen(Instr.jpc, 0, 0);			// ����������ת��������ѭ���ĵ�ַδ֪
		if (symtype == Symbol.dosym)
			nextsym();
		else
			Err.report(18);						// ȱ��do
		statement(fsys, lev);					// ����<���>
		interp.gen(Instr.jmp, 0, cx1);			// ��ͷ�����ж�����
		interp.code[cx2].a = interp.cx;			// ��������ѭ���ĵ�ַ����<�������>����
	}

	/**
	 * ����<�������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseBeginStatement(BitSet fsys, int lev) {
		BitSet nxtlev;
		
		nextsym();
		nxtlev = (BitSet) fsys.clone();
		nxtlev.set(Symbol.semicolon);
		nxtlev.set(Symbol.endsym);
		statement(nxtlev, lev);
		// ѭ������{; <���>}��ֱ����һ�����Ų�����俪ʼ���Ż��յ�end
		while (statbegsys.get(symtype) || symtype == Symbol.semicolon) {
			if (symtype == Symbol.semicolon)
				nextsym();
			else
				Err.report(10);					// ȱ�ٷֺ�
			statement(nxtlev, lev);
		}
		if (symtype == Symbol.endsym)
			nextsym();
		else
			Err.report(17);						// ȱ��end��ֺ�
	}

	/**
	 * ����<�������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseIfStatement(BitSet fsys, int lev) {
		int cx1;
		BitSet nxtlev;
		
		nextsym();
		nxtlev = (BitSet) fsys.clone();
		nxtlev.set(Symbol.thensym);					// �������Ϊthen��do ???
		nxtlev.set(Symbol.dosym);
		condition(nxtlev, lev);						// ����<����>
		if (symtype == Symbol.thensym)
			nextsym();
		else
			Err.report(16);							// ȱ��then
		cx1 = interp.cx;							// ���浱ǰָ���ַ
		interp.gen(Instr.jpc, 0, 0);				// ����������תָ���ת��ַδ֪����ʱд0
		statement(fsys, lev);						// ����then������
		interp.code[cx1].a = interp.cx;				// ��statement�����cxΪthen�����ִ��
													// ���λ�ã�������ǰ��δ������ת��ַ
	}

	/**
	 * ����<���̵������>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseCallStatement(BitSet fsys, int lev) {
		int i;
		nextsym();
		if (symtype == Symbol.ident) {
			i = table.position(sym.id);
			if (i == 0) {
				Err.report(11);				// ����δ�ҵ�
			} else {
				Table.Item item = table.get(i);
				if (item.kind == Table.Item.procedur)
					interp.gen(Instr.cal, lev - item.level, item.adr);
				else
					Err.report(15);			// call���ʶ��ӦΪ����
			}
			nextsym();
		} else {
			Err.report(14);					// call��ӦΪ��ʶ��
		}
	}

	/**
	 * ����<д���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseWriteStatement(BitSet fsys, int lev) {
		BitSet nxtlev;

		nextsym();
		if (symtype == Symbol.lparen) {
			do {
				nextsym();
				nxtlev = (BitSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				nxtlev.set(Symbol.comma);
				expression(nxtlev, lev);
				interp.gen(Instr.opr, 0, 14);
			} while (symtype == Symbol.comma);
			
			if (symtype == Symbol.rparen)
				nextsym();
			else
				Err.report(33);				// write()��ӦΪ�������ʽ
		}
		interp.gen(Instr.opr, 0, 15);
	}

	/**
	 * ����<�����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseReadStatement(BitSet fsys, int lev) {
		int i;
		
		nextsym();
		if (symtype == Symbol.lparen) {
			do {
				nextsym();
				if (symtype == Symbol.ident)
					i = table.position(sym.id);
				else
					i = 0;
				
				if (i == 0) {
					Err.report(35);			// read()��Ӧ���������ı�����
				} else {
					Table.Item item = table.get(i);
					if (item.kind == Table.Item.variable) {
						Err.report(32);		// read()�еı�ʶ�����Ǳ���, thanks to amd
					} else {
						interp.gen(Instr.opr, 0, 16);
						interp.gen(Instr.sto, lev-item.level, item.adr);
					}
				}
				
				nextsym();
			} while (symtype == Symbol.comma);
		} else {
			Err.report(34);					// ��ʽ����Ӧ��������
		}
		
		if (symtype == Symbol.rparen) {
			nextsym();
		} else {
			Err.report(33);					// ��ʽ����Ӧ��������
			while (!fsys.get(symtype))
				nextsym();
		}
	}

	/**
	 * ����<��ֵ���>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void parseAssignStatement(BitSet fsys, int lev) {
		int i;
		BitSet nxtlev;
		
		i = table.position(sym.id);
		if (i > 0) {
			Table.Item item = table.get(i);
			if (item.kind == Table.Item.variable) {
				nextsym();
				if (symtype == Symbol.becomes)
					nextsym();
				else
					Err.report(13);					// û�м�⵽��ֵ����
				nxtlev = (BitSet) fsys.clone();
				expression(nxtlev, lev);
				// expression��ִ��һϵ��ָ������ս�����ᱣ����ջ����ִ��sto������ɸ�ֵ
				interp.gen(Instr.sto, lev - item.level, item.adr);
			} else {
				Err.report(12);						// ��ֵ����ʽ����
			}
		} else {
			Err.report(11);							// ����δ�ҵ�
		}
	}

	/**
	 * ����<���ʽ>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void expression(BitSet fsys, int lev) {
		int addop;
		BitSet nxtlev;
		
		// ����[+|-]<��>
		if (symtype == Symbol.plus || symtype == Symbol.minus) {
			addop = symtype;
			nextsym();
			nxtlev = (BitSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			term(nxtlev, lev);
			if (addop == Symbol.minus)
				interp.gen(Instr.opr, 0, 1);
		} else {
			nxtlev = (BitSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			term(nxtlev, lev);
		}
		
		// ����{<�ӷ������><��>}
		while (symtype == Symbol.plus || symtype == Symbol.minus) {
			addop = symtype;
			nextsym();
			nxtlev = (BitSet) fsys.clone();
			nxtlev.set(Symbol.plus);
			nxtlev.set(Symbol.minus);
			term(nxtlev, lev);
			if (addop == Symbol.plus)
				interp.gen(Instr.opr, 0, 2);
			else
				interp.gen(Instr.opr, 0, 3);
		}
	}

	/**
	 * ����<��>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void term(BitSet fsys, int lev) {
		int mulop;
		BitSet nxtlev;

		// ����<����>
		nxtlev = (BitSet) fsys.clone();
		nxtlev.set(Symbol.times);
		nxtlev.set(Symbol.slash);
		factor(nxtlev, lev);
		
		// ����{<�˷������><����>}
		while (symtype == Symbol.times || symtype == Symbol.slash) {
			mulop = symtype;
			nextsym();
			factor(nxtlev, lev);
			if (mulop == Symbol.times)
				interp.gen(Instr.opr, 0, 4);
			else
				interp.gen(Instr.opr, 0, 5);
		}
	}

	/**
	 * ����<����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void factor(BitSet fsys, int lev) {
		BitSet nxtlev;
		
		test(facbegsys, fsys, 24);			// ������ӵĿ�ʼ����
		// the original while... is problematic: var1(var2+var3)
		// thanks to macross
		// while(inset(sym, facbegsys))
		if (facbegsys.get(symtype)) {
			if (symtype == Symbol.ident) {			// ����Ϊ���������
				int i = table.position(sym.id);
				if (i > 0) {
					Table.Item item = table.get(i);
					switch (item.kind) {
					case Table.Item.constant:			// ����Ϊ����
						interp.gen(Instr.lit, 0, item.val);
						break;
					case Table.Item.variable:			// ����Ϊ����
						interp.gen(Instr.lod, lev - item.level, item.adr);
						break;
					case Table.Item.procedur:			// ����Ϊ����
						Err.report(21);				// ����Ϊ����
						break;
					}
				} else {
					Err.report(11);					// ��ʶ��δ����
				}
				nextsym();
			} else if (symtype == Symbol.number) {	// ����Ϊ�� 
				int num = sym.num;
				if (num > PL0.amax) {
					Err.report(31);
					num = 0;
				}
				interp.gen(Instr.lit, 0, num);
				nextsym();
			} else if (symtype == Symbol.lparen) {	// ����Ϊ���ʽ
				nextsym();
				nxtlev = (BitSet) fsys.clone();
				nxtlev.set(Symbol.rparen);
				expression(nxtlev, lev);
				if (symtype == Symbol.rparen)
					nextsym();
				else
					Err.report(22);					// ȱ��������
			} else {
				// �����ȴ�ʩ
				test(fsys, facbegsys, 23);
			}
		}
	}

	/**
	 * ����<����>
	 * @param fsys ������ż�
	 * @param lev ��ǰ���
	 */
	private void condition(BitSet fsys, int lev) {
		int relop;
		BitSet nxtlev;
		
		if (symtype == Symbol.oddsym) {
			// ���� ODD<���ʽ>
			nextsym();
			expression(fsys, lev);
			interp.gen(Instr.opr, 0, 6);
		} else {
			// ����<���ʽ><��ϵ�����><���ʽ>
			nxtlev = (BitSet) fsys.clone();
			nxtlev.set(Symbol.eql);
			nxtlev.set(Symbol.neq);
			nxtlev.set(Symbol.lss);
			nxtlev.set(Symbol.leq);
			nxtlev.set(Symbol.gtr);
			nxtlev.set(Symbol.geq);
			expression(nxtlev, lev);
			if (symtype == Symbol.eql || symtype == Symbol.neq 
					|| symtype == Symbol.lss || symtype == Symbol.leq
					|| symtype == Symbol.gtr || symtype == Symbol.geq) {
				relop = symtype;
				nextsym();
				expression(fsys, lev);
				switch (relop) {
				case Symbol.eql:
					interp.gen(Instr.opr, 0, 8);
					break;
				case Symbol.neq:
					interp.gen(Instr.opr, 0, 9);
					break;
				case Symbol.lss:
					interp.gen(Instr.opr, 0, 10);
					break;
				case Symbol.geq:
					interp.gen(Instr.opr, 0, 11);
					break;
				case Symbol.gtr:
					interp.gen(Instr.opr, 0, 12);
					break;
				case Symbol.leq:
					interp.gen(Instr.opr, 0, 13);
					break;
				}
			} else {
				Err.report(20);
			}
		}
	}
	
	void debug(String msg) {
		System.out.println("*** debug : " + msg);
	}
}
