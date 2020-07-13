// SPDX-License-Identifier: MIT
package util

import (
	"fmt"
	"regexp"
	"strings"
)

// Filepathmatch - This method provides ANT like selectors.
// See https://ant.apache.org/manual/dirtasks.html
// For example: "**/a*.txt" will accept
//               - "/home/tester/xyz/a1234.txt"
//               - "a1b.txt"
//
func Filepathmatch(path string, pattern string) (result bool) {

	// Let's turn the ant style pattern into a regexp:
	doublestarPatterns := strings.Split(pattern, "**/")

	for i, subElement := range doublestarPatterns {
		// esacpe . with backslash, * to .*
		doublestarPatterns[i] = strings.Replace(strings.Replace(subElement, ".", "\\.", -1), "*", ".*", -1)
	}
	regexpPattern := "^" + strings.Join(doublestarPatterns, ".*/") + "$"

	// add a './' in front of the path except it starts with a '/'
	// so e.g. "**/.git/*" will also match the current working directory and not only subdirectories
	if !strings.HasPrefix(path, "/") && strings.Contains(pattern, "/") {
		path = "./" + path
	}

	matched, err := regexp.MatchString(regexpPattern, path)
	if err != nil {
		fmt.Println(err, matched)
	}

	return matched
}
