package org.ibcb.xnat.redaction.helpers;

public class Pair<L, R>
{
    private L left;
    private R right;
 
    public void setLeft(L left){
    	this.left=left;
    }
    
    public void setRight(R right){
    	this.right=right;
    }
    
    public R getRight()
    {
         return right;
    }
 
    public L getLeft()
    {
        return left;
    }
 
    public Pair(final L left, final R right)
    {
        this.left = left;
        this.right = right;
    }
 
    public final boolean equals(Object o)
    {
        if (!(o instanceof Pair)) return false;
 
        Pair other = (Pair) o;
        
        if (getLeft() != null && !getLeft().equals(other.getLeft())) {
            return false;
        } else if (getLeft() == null && other.getLeft() != null) {
            return false;
        } else if (getRight() != null && !getRight().equals(other.getRight())) {
            return false;
        } else if (getRight() == null && other.getRight() != null) {
            return false;
        } else {
            return true;
        }
    }
 
    public int hashCode()
    {
        int hLeft = getLeft() == null ? 0 : getLeft().hashCode();
        int hRight = getRight() == null ? 0 : getRight().hashCode();
 
        return hLeft + (57 * hRight);
    }
}
