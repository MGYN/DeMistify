package backwardslicing;

import java.util.ArrayList;
import java.util.List;

import graph.DGraph;
import graph.ValuePoint;

public class BackwardController {
	static BackwardController sc = new BackwardController();

	public static BackwardController getInstance() {
		return sc;
	}

	private BackwardController() {

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public List<BackwardContext> doBackWard(ValuePoint vp, DGraph dg) {

		List<BackwardContext> bcs = new ArrayList<BackwardContext>();
		bcs.add(new BackwardContext(vp, dg));
		BackwardContext bc;
		while (true) {
			bc = null;
			for (BackwardContext tmp : bcs) {

				// 获取当前的BackwardContext（可能是new的那个）
				if (!tmp.backWardHasFinished()) {
					bc = tmp;
					break;
				}
			}
			if (bc == null) {
				break;
			}
			// 将进行一步回溯的加入bcs
			bcs.addAll(bc.oneStepBackWard());
			// bc.oneStepBackWard();

		}
		return bcs;

	}

}
