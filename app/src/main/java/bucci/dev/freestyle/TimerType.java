/*
 Copyright Michal Buczek, 2014
 All rights reserved.

This file is part of Freestyle Timer.

    Freestyle Timer is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Freestyle Timer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Freestyle Timer; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


package bucci.dev.freestyle;

public enum TimerType {
    BATTLE(0),
    QUALIFICATION(1),
    ROUTINE(2);

    private int value;

    TimerType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
