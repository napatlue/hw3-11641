
public class IndriParam {
  public float mu;
  public float lambda;
  public String smoothing;
  
  public IndriParam(float mu,float lambda, String smoothing) {
    // TODO Auto-generated constructor stub
    this.mu=mu;
    this.lambda=lambda;
    this.smoothing=smoothing;
  }
  
  public boolean validateParam(){
    if(mu < 0 || lambda <0 || lambda > 1 || (!smoothing.equalsIgnoreCase("ctf") && !smoothing.equalsIgnoreCase("df")))
    {
      return false;
    }
    return true;
  }
}
