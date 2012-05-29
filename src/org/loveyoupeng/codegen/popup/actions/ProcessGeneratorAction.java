package org.loveyoupeng.codegen.popup.actions;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.ide.IDE;

public class ProcessGeneratorAction implements IObjectActionDelegate {

	private ICompilationUnit selectCompilationUnit;
	private Shell shell;

	/**
	 * Constructor for Action1.
	 */
	public ProcessGeneratorAction() {
		super();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		// System.out.println(selectCompilationUnit.getClass().getCanonicalName()
		// + " : " + (selectCompilationUnit instanceof ICompilationUnit));
		IProject javaProject = selectCompilationUnit.getResource().getProject();
		try {
			SelectionDialog typeDialog = JavaUI.createTypeDialog(shell,
					new ProgressMonitorDialog(shell), javaProject,
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, false);
			typeDialog.open();
			Object[] results = typeDialog.getResult();
			selectCompilationUnit = ((IType) results[0]).getCompilationUnit();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CompilationUnit unit = parse(selectCompilationUnit);
		modify(unit);
	}

	private void modify(CompilationUnit unit) {
		// ASTRewrite rewrite = ASTRewrite.create(unit.getAST());
		// ImportDeclaration importStat = unit.getAST().newImportDeclaration();
		// Name name = importStat.getAST().newName("test.Request");
		// importStat.setName(name);
		try {
			ICompilationUnit cu = selectCompilationUnit; // content is

			IFile file = (IFile) cu.getResource().getWorkspace().getRoot()
					.findMember(cu.getPath());
			IWorkbench wb = PlatformUI.getWorkbench();
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			IWorkbenchPage page = win.getActivePage();
			HashMap map = new HashMap();
			map.put(IDE.EDITOR_ID_ATTR, "org.eclipse.ui.DefaultTextEditor");
			IMarker marker = file.createMarker(IMarker.TEXT);
			marker.setAttributes(map);
			// page.openEditor(marker); //2.1 API
			IDE.openEditor(page, file);
			// creation of a Document
			// "public class X {\n}"
			String source = cu.getSource();
			Document document = new Document(source);

			// creation of DOM/AST from a ICompilationUnit
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setSource(cu);
			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

			// start record of the modifications
			astRoot.recordModifications();

			// // modify the AST
			// TypeDeclaration typeDeclaration =
			// (TypeDeclaration)astRoot.types().get(0);
			// SimpleName newName = astRoot.getAST().newSimpleName("Y");
			// typeDeclaration.setName(newName);
			ImportDeclaration importStat = astRoot.getAST()
					.newImportDeclaration();
			Name name = importStat.getAST().newName("hahaha.Test");
			importStat.setName(name);

			addImports(astRoot, importStat);

			// computation of the text edits
			TextEdit edits = astRoot.rewrite(document, cu.getJavaProject()
					.getOptions(true));

			// computation of the new source code
			edits.apply(document);
			String newSource = document.get();

			// update of the compilation unit
			cu.getBuffer().setContents(newSource);

			cu.commitWorkingCopy(true, null);

		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void addImports(CompilationUnit astRoot,
			ImportDeclaration importStat) {
		astRoot.imports().add(importStat);
	}

	private CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		Object target = ((IStructuredSelection) selection).getFirstElement();
		if (target instanceof ICompilationUnit)
			selectCompilationUnit = (ICompilationUnit) target;
	}

}
