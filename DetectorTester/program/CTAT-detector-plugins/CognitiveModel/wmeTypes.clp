(call java.lang.System setProperty "UseStudentValuesFact" "true")
(defglobal ?*n* = 1)    ; or is defglobal for defining a constant? needed so each fact can be unique
(set-maximum-chain-depth 7)
(set-hint-policy "Bias Hints by Prior Error Only")
(call (engine) setStrategyByName "buggy-rules-normal-salience")
;(call (engine) setSkipWhyNotSaves TRUE)

(deftemplate studentValues
	(slot selection)
	(slot action)
	(slot input))

(deftemplate hint (slot now))

(deftemplate custom-fields
    (slot preEqSys)
    (slot preEqStu)
    (slot postEqSys)
    (slot postEqStu)
    ;    (slot curLine)
    (slot act)      ; probably good to avoid "action" as a DataShop column
    (slot transf)
;    (slot strategic)
;    (slot varLeft)
;    (slot varRight)
;    (slot constLeft)
;    (slot constRight)
;    (slot zeroLeft)
;    (slot zeroRight)
;    (slot prodLeft)
;    (slot prodRight)
;    (slot negVarLeft)
;    (slot negVarRight)
;    (slot negConstLeft)
;    (slot negConstRight)
	(slot preEqProps)
    (slot transfProps)
    (slot pKnowValues)
            )

;(deftemplate Skill
;    (slot name)
;    (slot category)
;    (slot description)
;    (slot label)
;    (slot opportunityCount)
;    (slot pGuess)
;    (slot pKnown)
;    (slot pLearn)
;    (slot pSlip))

;;----------------------------------------------------------------------------------------
;;
;; Representing equations
;;
;;
;; Equations are represented in working memory as follows:
;; An equation (template: equation) has two sides, which are expressions (template: expr)
;; Expressions are lists of terms;  it is implied that the expression is the sum of these terms.
;;    (Should we perhaps call them term-lists instead of expressions?)
;; There are 3 kinds of terms:
;;    - simple terms (i.e., constant terms or variable terms)  (template: simple-term)
;;    - product-terms (template: product-term), and 
;;    - quotient-terms (template: quotient-term)
;; Product-terms and quotient-terms each have factors; factors are always expressions, to make the
;;    structure fully recursive. In product-terms, the factors represent the expressions being multiplied,
;;    in quotient-terms they represent the dividend expression and the divisor expression, respectively.
;; (Why is it that we have product-term and quotient-term but not additive-term and subtractive-term? Wouldn't
;;     one expect symmetry?)
;; TO DO: representing an expression that is 0 is currently done with a simple term that has coefficient 0.
;;        But wouldn't it be better to set the term list to the empty list?
;;

(deftemplate problem
    (slot cur-transformation)
    (slot hint-transformation)      ; used only by hint-move-simple-term
    (slot prov-transformation)     ; a transformation can be in provisional status, meaning that some of
                                          ;   the details are tentative and may change (this happens only when doing pre-explanations)
                                     ; e.g., after entering "Add" the provisional transformation may be
                                     ; "Add 3 to both sides" but depending on later student input this may
                                     ;     change to "Add 4x to both sides" (the "Add" part will not change,
                                     ;     however, once that has been confirmed as correct input by the
                                     ;     student)
    (slot cur-equation)              ; the equation as it was at the start of the current transformation (i.e., this
                                     ;     slot is updated each time a transformation is completed)
    (slot printed (default FALSE))   ; for development only
    (multislot open-lines)
    (multislot closed-lines)
    (multislot pre-expl-groups      ; stores the names of the groups in the interface - used for showing and hiding them all
                                     ; mostly needed for testing with going back to the start state
;        (default preExpl1Group preExpl2Group preExpl3Group
;                 preExpl4Group preExpl5Group preExpl6Group
;                 preExpl7Group preExpl8Group preExpl9Group)
            )
    (multislot solve-groups  
            ; commentary
;        (default solve1Group solve2Group solve3Group
;                 solve4Group solve5Group solve6Group
;                 solve7Group solve8Group solve9Group)
            )
    (multislot post-expl-groups
;        (default postExpl1Group postExpl2Group postExpl3Group
;                 postExpl4Group postExpl5Group postExpl6Group
;                 postExpl7Group postExpl8Group postExpl9Group)
            )
    (multislot solve-groups)
    (multislot post-expl-groups)
    (multislot steps)
    (slot checked-answer-p)          ; used to do things in a non-model-tracing kind of way,
                                     ;    to improve the model's efficiency in recognizing errors
    (slot cur-step)
    (slot pre-explanations-p)
    (slot post-explanations-p)
    )

(deftemplate equation
    (multislot sides)
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))   ; so every equation is a unique fact
        )

(deftemplate expr
    (multislot terms)
    (slot type (default expr))
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))   ; so every expr is a unique fact
        )

(deftemplate simple-term
    (slot coeff)
    (slot var)
    (slot type (default simple-term))   ; bizarre but could not figure out other way to find out a fact's type
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))   ; so every term is a unique fact
    )

(deftemplate quotient-term
    (multislot factors)     ; should be expr-s --- but maybe they should not be called factors? seemed like an appropriate generic term
    (slot type (default quotient-term))
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))   ; so every term is a unique fact
        )

(deftemplate product-term       ; or generalize across quotient and product term?
    (multislot factors)     ; should be expr-s
    (slot type (default product-term))
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))   ; so every term is a unique fact
        )

;;----------------------------------------------------------------------------------------
;;
;; Representing transformations
;;
;; Explicit representation of transformations is necessary primarily to help with being
;;    flexible and/or configurable with respect to step skipping (i.e., the challenge is to deal flexibly
;;    with intermediate steps; but in a way that makes it easy to require them). Representing transformation may also
;;    help with the implementation of the undo facility although that is less clear. 
;;
;; A transformation represents application of one of the equation-solving operators, with simplification.
;; A transformation can therefore occupy multiple rows in the interface (up to 3).
;; Also, essentially the same transformation can occupy different numbers of rows in the interface,
;;    depending on whether the student does the simplification steps implicity, explicitly and at the same
;;    time (i.e., in the same row) to both sides, or explicitly and one-by-one.
;; Transformations are created by the rules that capture the main equation-solving operators (which can fire
;;     only when there is no current transformation - i.e., when the previous one has been completed).
;; At any point in time, there is a single "current transformation."  It is needed to support both implicit
;;     and explicit simplification, as described above.  The transformation records to what degree the
;;     simplification has happened already.
;; There are rules for creating transformations and rules for writing out transformations in the interface,
;;     which requires multiple steps (at least 2, at most 6) in the interface.
;;
;; The connection between transformation and rows in the interface is recorded in the lines.
;; --> Or at least, that is the intention.
;; --> The idea seems to be that each time a line is closed, a duplicate of the current transformation is
;;     stored with that line.
;; --> However, for a step-wise undo (step in the ITS sense of the terms), it would be necessary to record
;;     the steps.  E.g., create a stack of steps, copying the current transformation and perhaps other state
;;     info when a step is taken.
;;

;; Reminder: when this template is changed, the function copy-transf needs to change as well.
(deftemplate transformation
    (slot equation)                  ; the target of the transformation, i.e., what the equation will look
                                     ;    like once the transformation is complete
    (multislot to-be-simplified)     ; values can be: left, right;  indicares whether simplification still needs to occur
    (multislot written)              ; values can be: left, right;  indicates whether the simplified form
                                     ;    has been written
    (slot pre-explained)
    (slot post-explained)            ; indicates whether the post-explanation has happened yet (should happen on the
                                     ;    first line for this transformation)
    (slot focus)                     ; focus field is set in each cycle - it serves mainly to prune the
                                     ;      search, it seems
    (slot prev-left-val)             ; when transformation occupies multiple rows in the interface, the value
                                     ;    (i.e., the expression represented as string) in the previous row
                                     ;    corresponding to the current transformation; nil if the previous row
                                     ;    corresponds to a different transformation
                                     ; --> what is this information useful for?
    (slot prev-right-val)            ; analogous to prev-left-val
    (multislot description)          ; describes the transformation - needed for pre/post explanations
                                     ; expect three values: operation, number, side(s)
    (slot skip-expl-sel2)            ; operators that skip the second out of three explanations fields (e.g.,
                                     ;    distribute, combine like terms) must indicate that here (should be
                                     ;    selection name)
    (slot no-hints-p)                ; indicates we are in a chain that has no hints - within-chain use only
    (slot strategic-p (default TRUE))       ; so we can mark transformations that are not strategic
    (slot to-side)                   ; this is for add/subtract transformations only - which side are we
                                     ;      moving to?
;    (slot check-for-circle (default FALSE))          ; this enables the rule(s) that check(s) for repeated states to
;                                     ;    step in before the focus rule(s)
;                                     ; e.g., ax = bx + c --> ax - c = bx --> ax = bx + c
;                                     ; so the tutor can provide strategy feedback
;    (slot gave-circle-feedback (default -1))    ; keep track of how many times strategy feedback has been
;                                     ; given (so tutor does not repeat it too many times)
;                                     ;
    (slot fact-nr (default-dynamic (bind ?*n* (+ 1 ?*n*))))
               ; so that every transformation is a fact with unique slot values - Jess refuses to create copies
            )


;;----------------------------------------------------------------------------------------
;;
;; Representing steps so they can be undone by the student
;;
;; To undo a step, need to know:
;; - interface component that was filled in
;; - interface components that were revealed (or hidden??)
;; - lists of open and closed rows
;; - the current transformation at the beginning of the step
;; - the current equation at the beginning of the step (actually, could that simply be the one in the problem? no)

;; The idea is that to undo a step, one needs information only from the corresponding step fact.
;; So it should record the step (so the Flash interface component can be reset and the correspodning value in WM set to NIL)
;;    plus the pre-state, i.e., the state of various things just prior to the step.
;; So every chain needs a first rule that records the pre-state. 
;; 
;;
(deftemplate step
    (multislot interface-elements)          ; component fact, not name of the component
    (multislot pre-open-list)
    (multislot pre-closed-list)
    (slot pre-transformation)
    (slot pre-equation)
    (slot provisional-p)              ; whether the transformation should be restored as a provisional transformation
                                      ;    or as a regular transformation
    (slot revealed-interface-group)
    )

;;----------------------------------------------------------------------------------------
;;
;; Structures for program control
;;

;; EXPLAIN-OP
;;
;; Explain-ops  are transient facts - they exist temporarily, only within a single chain (i.e.,
;;    are retracted in the same chain in which they are created and not kept in working memory across
;;    cycles).
;; Currently (July 31), they are used only for pre-explanations, to capture things that need to happen
;;    on the right-hand-side once the student SAI has been correctly predicted. In particular, it
;;    makes the step undoable in the usual manner and stores the transformation as a provisional 
;;    transformation when needed (i.e., when the explain-op's prov-transformation slot contains a
;;    transformation).
;; TO DO:  consider whether writing a Jess function for the common rhs actions might not be better.
;; Currently, an add/subtract transformation is provisional until the operand (misnomed as "number")
;;    has been determined. Doesn't mean there are always multiple options, just that the model is not
;;    structured to collect all possible options and then decide based on what options are collected.
;;    (Could be neat to build a "strategic" model that could do just that, i.e., collect the options,
;;    reason about which one is best, and then decide.)
;; TO DO:  combining like terms is provisional until the sides have been specified (so how to deal with
;;    with.)
;; 
;; Currently (July 31), pre-explain-add-op creates an explain-op 
;; pre-explain-distribute-op also creates an explain-op
;; 
(deftemplate explain-op          ; TO DO:  call this subgoal?  but have not been consistent with subgoals 
    (slot interface-element)
    (slot input)
    (slot prov-transformation)
    (slot lock)                  ; in distribute ops, want to lock the num field
    )


;;----------------------------------------------------------------------------------------
;;
;; Representing the interface in working memory
;;

;; Option
;; interface-elements is a list of line facts
;; each line has pre-explanation, equation, and post-explanation slots
;; these slots contain lists of component names?
;; Or is this needlessly complicated?

(deftemplate line
    	(multislot pre-explanations)
    (multislot solution-steps)
    (multislot post-explanations)
    (multislot groups)
    )

(deftemplate interface-element
    (slot name)
    (slot value)
    )

; tell productionRules file that templates have been parsed
(provide wmeTypes)