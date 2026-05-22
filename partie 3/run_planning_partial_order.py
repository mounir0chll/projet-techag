import sys
from pathlib import Path


AIMA_DIR = Path(__file__).resolve().parent / "aima-python"
sys.path.insert(0, str(AIMA_DIR))

from planning import (  # noqa: E402
    GraphPlan,
    PartialOrderPlanner,
    linearize,
    socks_and_shoes,
    spare_tire,
    three_block_tower,
)


def action_name(action):
    return str(action)


def run_pop_demo():
    problem = socks_and_shoes()
    planner = PartialOrderPlanner(problem)
    planner.execute(display=False)

    levels = list(reversed(list(planner.toposort(planner.convert(planner.constraints)))))
    print("=== Partie 3: AIMA Partial Order Planner ===")
    print("Probleme teste: socks_and_shoes")
    print("Plan partiellement ordonne par niveaux:")
    for index, level in enumerate(levels, start=1):
        names = ", ".join(action_name(action) for action in level)
        print(f"  Niveau {index}: {names}")

    expected_order = [
        {"Start"},
        {"LeftSock", "RightSock"},
        {"LeftShoe", "RightShoe"},
        {"Finish"},
    ]
    actual_order = [{action.name for action in level} for level in levels]
    assert actual_order == expected_order, actual_order
    print("Verification POP: OK")


def run_graphplan_smoke_tests():
    print("\n=== Tests GraphPlan AIMA ===")
    spare_solution = [action_name(action) for action in linearize(GraphPlan(spare_tire()).execute())]
    print("Spare tire:", spare_solution)
    assert "Remove(Flat, Axle)" in spare_solution
    assert "Remove(Spare, Trunk)" in spare_solution
    assert "PutOn(Spare, Axle)" in spare_solution

    tower_solution = [action_name(action) for action in linearize(GraphPlan(three_block_tower()).execute())]
    print("Three block tower:", tower_solution)
    assert tower_solution
    print("Verification GraphPlan: OK")


if __name__ == "__main__":
    run_pop_demo()
    run_graphplan_smoke_tests()
