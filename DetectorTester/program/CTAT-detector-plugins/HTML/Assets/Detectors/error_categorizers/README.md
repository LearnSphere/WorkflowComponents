**Documentation and to-do items for error categorizers**


- **Documentation (to update)**
	- Description: keeps a count of student errors, categorized under a particular canonicalization scheme, and may also keep track of concrete examples of student errors
	- Outputs: mapping from observed student error categories to
		- running, per-student count of observations
		- raw examples (i.e., non-canonicalized)
	- Potential extensions: 
		- Recency (i.e., decay function)
		- associate every update from an **error categorizer** with a reference to the particular problem instance (I believe this is already included on TutorShop's end)
	- Current canonicalization schemes include:
		- Simple canonicalization: line-to-line transition representation
			- **Example 1**
				- input:   x + 6 = 15   ->   x + 6 - 6 = 15
				- output:  x + a = b    ->   x + a - a = b
			- **Example 2**
				- input:   x + 6 = 15 -> 6 = 15
				- output:  x + a = b  -> a = b
			- **Example 3**
				- input:    x + 6 = 15 -> x = 15 + 6
				- output:   x + a = b  -> x = b + a

		- Simple canonicalization: track changes
			- ![example](https://raw.githubusercontent.com/d19fe8/CTAT-detector-plugins/master/Screen%20Shot%202017-01-22%20at%204.14.46%20PM.png)
			- ![example](https://raw.githubusercontent.com/d19fe8/CTAT-detector-plugins/master/Screen%20Shot%202017-01-22%20at%204.18.39%20PM.png)
			- ![example](https://raw.githubusercontent.com/d19fe8/CTAT-detector-plugins/master/Screen%20Shot%202017-01-22%20at%204.23.10%20PM.png)

			- **Example 1**
				- input:   x + 6 = 15   ->   x + 6 - 6 = 15
				- output:  x + a **- a** = b
			- **Example 2**
				- input:   x + 6 = 15 -> 6 = 15 
				- output:  **[x +]** a = b
			- **Example 3**
				- input:   x + 6 = 15 -> x = 15 + 6
				- output:  x **[+ 6]** = 15 **+ 6**

		- Change-based canonicalization: an extra layer over "track changes", which shows **only what changes between two lines in Lynnette** (and thus produces **fewer** categories, more focused on abstract **transformations**)

			- ![example](https://raw.githubusercontent.com/d19fe8/CTAT-detector-plugins/master/Screen%20Shot%202017-01-22%20at%204.24.00%20PM.png)

			- **Example 1**
				- input:   x + 6 = 15   ->   x + 6 - 6 = 15
				- output:  ... - a = ...
			- **Example 2**
				- input:   x + 6 = 15 -> 6 = 15
				- output:  [x] ... = ...
			- **Example 3**
				- input:    x + 6 + 3 = 15 -> x + 3 = 15 + 6
				- output:   ... [+ 6] ... = ... + 6
			- **Example 4**
				- input:    x + 6 + 3 - 2 = 15 -> x + 6 + 3 - 2 = 15 + 6
				- output:   ... [+ 6] ... = ... + 6

	- In-development canonicalization schemes include:
		- Labeled error categories (verbal category descriptions, which DO NOT infer underlying misconceptions)
			- **Example 1**
				- input:  
				- output:
			- **Example 2**
				- input:  
				- output:
			- **Example 3**
				- input:  
				- output:
		- Inferred misconceptions (verbal category descriptions, which DO infer underlying misconceptions)
			- Misconception labeling scheme 1
				- **Example 1**
					- input:  
					- output:
				- **Example 2**
					- input:  
					- output:
				- **Example 3**
					- input:  
					- output:
			- Misconception labeling scheme 2
				- **Example 1**
					- input:  
					- output:
				- **Example 2**
					- input:  
					- output:
				- **Example 3**
					- input:  
					- output:
			- Misconception labeling scheme 3
				- **Example 1**
					- input:  
					- output:
				- **Example 2**
					- input:  
					- output:
				- **Example 3**
					- input:  
					- output:
			- ....
			
	- Don't like any of the above canonicalization schemes (and/or misconception labeling schemes)? Please contribute to this collection by creating your own canonicalization function, and feel free to use one of our examples as a guiding template.
