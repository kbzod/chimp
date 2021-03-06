# CHIMP 1.2 Users Manual

CHIMP is a program that stores passwords for you.

CHIMP is pretty simple to use. Still, everything you might want to know about
how to use it is contained herein.

## Overview

When you first start CHIMP, you'll be at the main screen, with an empty list
just waiting for you. You will fill up the list with items, each of which
describes some password or other secret piece of information you want to store.
Every item has a title which you refer to them by: things like "Facebook",
"Twitter", "my girlfriend's computer", whatever.

CHIMP can save all your items in a file. The file itself will be encrypted with
a password that you choose. The only way that the file can be used again in
CHIMP (or read by anybody else, for that matter) is if it is decrypted with the
same password. This way, the passwords that the file contains are safer than
they would be if you just wrote them down somewhere, or kept them in an
ordinary text file on your computer. But be careful: If you forget the password
you use for your file, you won't be able to open that file again.

## Managing Entries

To start using CHIMP, add a new entry by selecting "Add New Item..." from the
Edit menu. You'll see the item editing dialog. Here is an explanation of what
you can enter.

* Title: A name for the item. You must provide a unique title for the item.
* Resource: The URL for the website that the item is for, or the computer name,
  or whatever you feel like putting in that tells you what the item is for.
  This is an optional entry.
* Username: If applicable, your login name for the resource.
* Password: The password! You won't see what you type in unless you unhide
  passwords (see below).
* Comments: Any comments you feel like adding to the item.

Press OK to add your new item. Press Cancel to ... um ... cancel. Press Apply
to add the item but to keep working on it.

To change or just look at an already existing item, double-click it in the list
or select "Modify..." from the Edit menu. This works just like adding a new
item.

To delete an item, select it and then select "Remove" from the Edit menu.

Normally when you are looking at an item the password is hidden from view. If
you want to see the actual password, then uncheck the "Hide Passwords" item in
the Options menu. From then on, when you work with an item, the password will
be plainly visible. If you think somebody is going to be peeking over your
shoulder while you're using CHIMP, you should probably leave that checked.

## Saving and Loading

Once you have some entries, you probably will want to save them. Select "Save"
or "Save As..." from the File menu. Pick your directory and file name; it can
be anything you want. Try using something tricky, like the name of a
spreadsheet or something, so that nosy people can't guess which file your stuff
is in. Then, provide a password for encrypting the file.

You can opt to save the file without any password by pressing the "No Password"
button. This will save everything, including your passwords, in plain text for
everybody to see, with no encryption.

After providing the password for encrypting the file, your information will be
saved. You can open it later by selecting "Open..." from the File menu.

## Using Your Passwords

So, how do you actually use the passwords you have hidden away using CHIMP?
One way is to unhide passwords by unchecking the "Hide Passwords" item in the
Options menu, then just opening the item you want and looking at the password.
But that's kind of unsafe if other people are wandering around.

A better way is to do this. Access whatever it is you need the password for;
e.g., go to the website or open the ssh session. Enter your username as usual.
Then get ready to enter your password. Now, in CHIMP, highlight the entry that
contains the password you want. Then, select "Copy Item to Clipboard" from the
Edit menu. This will copy your password into the clipboard. Then, paste like
you normally would into where you would type your password. Voila! Not only did
you save typing, but no one saw what your password was.

(Note: on Unix, this will enter the password as the clipboard selection, as
opposed to the "primary selection". This means that it won't be pasted if you
middle-click in your browser. Try using the Paste options from the menu or
using the equivalent keyboard shortcut, e.g., Ctrl+V.)

There is a risk though. Your password is still on the clipboard, and if
someone were to paste into, say, a text editor while you were away from your
computer, your password would show up plain as day. So, for security purposes,
you should clear the clipboard after you transfer your password. To do this,
you can select "Clear Clipboard" from the Edit menu.

## Menu Reference

### File Menu

* New - Clears CHIMP and starts a new list
* Open... - Opens a file
* Save - Saves the current list with its current file name
* Save As... - Saves the current list with a new file name
* Exit - Quit CHIMP

### Edit Menu

* Add New Item... - Add a new entry to the list
* Modify... - Edit the highlighted entry
* Remove - Remove the highlighted entry
* Copy Item to Clipboard - Copy the password in the highlighted entry to the
  clipboard
* Clear Clipboard - Clear the contents of the clipboard

### Options Menu

* Hide Passwords - Toggle whether passwords in entries are plainly shown or not

### Help Menu

* About - Some CHIMP information
