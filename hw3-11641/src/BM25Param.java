
public class BM25Param {
  public float k1;
  public float b;
  public float k3;
  
  public BM25Param(float k1,float b, float k3) {
    // TODO Auto-generated constructor stub
    this.k1=k1;
    this.b=b;
    this.k3=k3;
  }
  
  public boolean validateParam(){
    if(k1<0 || k3 < 0 || b <0 || b > 1)
    {
      return false;
    }
    return true;
  }
}
