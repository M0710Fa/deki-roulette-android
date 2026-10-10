package com.basekeita.roulette.data.definition.model

/** 盤面を回す向き。画面上の時計回りが正（回転角が増える向き）で、反時計回りでは累積角が減る。 */
enum class SpinDirection {
    CLOCKWISE,
    COUNTERCLOCKWISE,
}
